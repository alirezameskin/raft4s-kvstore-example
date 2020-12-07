package raft4s.demo.kvstore

import cats.data.Validated
import cats.effect.{ExitCode, IO}
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import io.odin._
import org.http4s.server.blaze._
import raft4s.demo.kvstore.utils.LogFormatter
import raft4s.rpc.grpc.io.implicits._
import raft4s.storage.memory.MemoryStorage
import raft4s.storage.rocksdb.RocksDBStorage
import raft4s.{Address, Configuration, Raft}

import java.io.File
import scala.concurrent.ExecutionContext.global
import scala.util.{Failure, Success, Try}

object KVApp extends CommandIOApp(name = "KVStore", header = "Simple KV store", version = "0.1") {

  implicit val logger: Logger[IO] =
    consoleLogger(formatter = new LogFormatter, minLevel = Level.Trace)

  private val path: Opts[File] =
    Opts
      .option[String]("storage", "Storage path", "s")
      .mapValidated { path =>
        Try(new File(path)) match {
          case Failure(exception)                                => Validated.invalidNel(exception.getMessage)
          case Success(file) if file.exists && !file.isDirectory => Validated.invalidNel(s"$path Does not exist")
          case Success(file)                                     => Validated.valid(file)
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
    val config = Configuration(options.local, options.servers)
    RocksDBStorage.open[IO](options.storagePath.getAbsolutePath).use { storage =>
      for {
        raft   <- Raft.make[IO](config, storage, new KvStateMachine())
        leader <- raft.start()
        _      <- logger.info(s"Cluster is started, the leader node is ${leader}")
        _ <- BlazeServerBuilder[IO](global)
          .bindHttp(options.httpPort, "localhost")
          .withHttpApp(http.HttpService.routes(raft))
          .serve
          .compile
          .drain
      } yield ExitCode.Success
    }
  }
}
