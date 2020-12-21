package raft4sdemo.kvstore.http

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import raft4s.Cluster
import raft4sdemo.kvstore.command.{DeleteCommand, GetCommand, SetCommand}

object HttpService {
  def routes(raft: Cluster[IO]) =
    HttpRoutes
      .of[IO] {
        case GET -> Root / "keys" / key =>
          Ok(raft.execute(GetCommand(key)))

        case req@POST -> Root / "keys" / key =>
          req.decode[String] { content =>
            Ok(raft.execute(SetCommand(key, content)))
          }

        case DELETE -> Root / "keys" / key =>
          Ok(raft.execute(DeleteCommand(key)))

      }
      .orNotFound
}
