package raft4s.demo.kvstore

import cats.data.Validated
import cats.effect.{ExitCode, IO, Resource}
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import io.odin._
import org.http4s.server.blaze._
import raft4s.demo.kvstore.utils.LogFormatter
import raft4s.rpc.grpc.io.implicits._
import raft4s.storage.file.{FileSnapshotStorage, FileStateStorage}
import raft4s.storage.rocksdb.RocksDBLogStorage
import raft4s.storage.{StateStorage, Storage}
import raft4s.{Address, Configuration, RaftCluster}

import java.io.File
import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.global
import scala.util.{Failure, Success, Try}

object KVApp extends CommandIOApp(name = "KVStore", header = "Simple KV store", version = "0.1") {

  implicit val logger: Logger[IO] =
    consoleLogger(formatter = new LogFormatter, minLevel = Level.Trace)

  private val path: Opts[Path] =
    Opts
      .option[String]("storage", "Storage path", "s")
      .mapValidated { path =>
        Try(new File(path)) match {
          case Failure(exception)                                => Validated.invalidNel(exception.getMessage)
          case Success(file) if file.exists && !file.isDirectory => Validated.invalidNel(s"$path Does not exist")
          case Success(file)                                     => Validated.valid(file.toPath)
        }
      }

  private val httpPort: Opts[Int] =
    Opts.option[Int]("http-port", "Http Port", "p")

  private val local: Opts[Address] = Opts
    .option[String]("local", "Local server", "l", metavar = "host:port")
    .mapValidated { string =>
      string.split(":", 2) match {
        case Array(s1, s2) if Try(s2.toInt).isSuccess => Validated.valid(Address(s1, s2.toInt))
        case _                                        => Validated.invalidNel(s"Invalid host:port : ${string}")
      }
    }

  private val servers: Opts[List[Address]] =
    Opts
      .options[String]("member", "another member", short = "m", metavar = "host:port")
      .mapValidated { strings =>
        strings.traverse { string =>
          string.split(":", 2) match {
            case Array(s1, s2) if Try(s2.toInt).isSuccess => Validated.valid(Address(s1, s2.toInt))
            case _                                        => Validated.invalidNel(s"Invalid host:port : ${string}")
          }
        }
      }
      .orEmpty

  override def main: Opts[IO[ExitCode]] = (path, httpPort, local, servers).mapN(AppOptions).map { options =>
    val config = Configuration(
      local = options.local,
      members = options.servers,
      followerAcceptRead = true,
      logCompactionThreshold = 10,
      electionMinDelayMillis = 0,
      electionMaxDelayMillis = 2000,
      heartbeatIntervalMillis = 2000,
      heartbeatTimeoutMillis = 10000
    )
    makeCluster(options.storagePath, config).use { cluster =>
      for {
        leader <- cluster.start
        _      <- logger.info(s"Cluster is started, the leader node is ${leader}")
        _ <- BlazeServerBuilder[IO](global)
          .bindHttp(options.httpPort, "localhost")
          .withHttpApp(http.HttpService.routes(cluster))
          .serve
          .compile
          .drain
      } yield ExitCode.Success
    }
  }

  private def makeCluster(storagePath: Path, config: Configuration): Resource[IO, RaftCluster[IO]] =
    for {
      storage      <- makeStorage(storagePath)
      stateMachine <- Resource.liftF(KvStateMachine.empty)
      cluster      <- RaftCluster.resource[IO](config, storage, stateMachine)
    } yield cluster

  private def makeStorage(path: Path): Resource[IO, Storage[IO]] =
    for {
      _               <- Resource.liftF(createDirectory(path))
      _               <- Resource.liftF(createDirectory(path.resolve("snapshots")))
      logStorage      <- RocksDBLogStorage.open[IO](path.resolve("logs"))
      stateStorage    <- Resource.pure[IO, StateStorage[IO]](FileStateStorage.open[IO](path.resolve("state")))
      snapshotStorage <- Resource.liftF(FileSnapshotStorage.open[IO](path.resolve("snapshots")))
    } yield Storage(logStorage, stateStorage, snapshotStorage)

  private def createDirectory(path: Path): IO[Unit] = IO.fromEither {
    Try(Files.createDirectory(path)).toEither match {
      case Right(_)                           => Right(())
      case Left(_) if Files.isDirectory(path) => Right(())
      case Left(error)                        => Left(error)
    }
  }
}
