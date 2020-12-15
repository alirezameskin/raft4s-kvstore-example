package raft4s.demo.kvstore

import cats.data.Validated
import cats.implicits._
import com.monovore.decline.Opts
import raft4s.Address

import java.io.File
import java.nio.file.Path
import scala.util.{Failure, Success, Try}

case class AppOptions(storagePath: Path, httpPort: Int, local: Address, servers: List[Address])

object AppOptions {
  def opts: Opts[AppOptions] = {

    val path: Opts[Path] =
      Opts
        .option[String]("storage", "Storage path", "s")
        .mapValidated { path =>
          Try(new File(path)) match {
            case Failure(exception)                                => Validated.invalidNel(exception.getMessage)
            case Success(file) if file.exists && !file.isDirectory => Validated.invalidNel(s"$path Does not exist")
            case Success(file)                                     => Validated.valid(file.toPath)
          }
        }

    val httpPort: Opts[Int] =
      Opts.option[Int]("http-port", "Http Port", "p")

    val local: Opts[Address] = Opts
      .option[String]("local", "Local server", "l", metavar = "host:port")
      .mapValidated { string =>
        string.split(":", 2) match {
          case Array(s1, s2) if Try(s2.toInt).isSuccess => Validated.valid(Address(s1, s2.toInt))
          case _                                        => Validated.invalidNel(s"Invalid host:port : ${string}")
        }
      }

    val servers: Opts[List[Address]] =
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

    (path, httpPort, local, servers).mapN(AppOptions.apply)
  }
}
