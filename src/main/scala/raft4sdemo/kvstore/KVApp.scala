package raft4sdemo.kvstore

import cats.effect.{ExitCode, IO, Resource}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import io.odin.{consoleLogger, Level}
import org.http4s.server.blaze._
import raft4s.{Configuration, Storage, Logger}
import raft4s.effect.RaftCluster
import raft4s.effect.storage.file.{FileSnapshotStorage, FileStateStorage}
import raft4s.effect._
import raft4s.effect.rpc.grpc.io.implicits._
import raft4s.effect.storage.rocksdb.RocksDBLogStorage
import raft4sdemo.kvstore.utils.LogFormatter

import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object KVApp extends CommandIOApp(name = "KVStore", header = "Simple KV store", version = "0.1") {

  implicit val logger: Logger[IO] =
    odinLogger(consoleLogger[IO](formatter = new LogFormatter, minLevel = Level.Trace))

  override def main: Opts[IO[ExitCode]] = AppOptions.opts.map { options =>
    makeCluster(options.storagePath, configuration(options)).use { cluster =>
      for {
        leader <- if (options.join.isEmpty) cluster.start else cluster.join(options.join.get)
        _      <- logger.info(s"Cluster is started, the leader node is ${leader} ${options.local}")
        _ <- BlazeServerBuilder[IO](global)
          .bindHttp(options.httpPort, "localhost")
          .withHttpApp(http.HttpService.routes(cluster))
          .serve
          .compile
          .drain
      } yield ExitCode.Success
    }
  }

  private def leaveCluster(cluster: RaftCluster[IO]): IO[Unit] =
    IO.sleep(FiniteDuration(25, TimeUnit.SECONDS)) *> IO(println("Start leaving")) *> cluster.leave *> IO.delay(sys.exit())

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
      stateStorage    <- Resource.pure[IO, FileStateStorage[IO]](FileStateStorage.open[IO](path.resolve("state")))
      snapshotStorage <- Resource.liftF(FileSnapshotStorage.open[IO](path.resolve("snapshots")))
    } yield Storage(logStorage, stateStorage, snapshotStorage)

  private def createDirectory(path: Path): IO[Unit] = IO.fromEither {
    Try(Files.createDirectory(path)).toEither match {
      case Right(_)                           => Right(())
      case Left(_) if Files.isDirectory(path) => Right(())
      case Left(error)                        => Left(error)
    }
  }

  private def configuration(options: AppOptions): Configuration =
    Configuration(
      local = options.local,
      members = options.servers,
      followerAcceptRead = true,
      logCompactionThreshold = 10,
      electionMinDelayMillis = 0,
      electionMaxDelayMillis = 2000,
      heartbeatIntervalMillis = 2000,
      heartbeatTimeoutMillis = 10000
    )
}
