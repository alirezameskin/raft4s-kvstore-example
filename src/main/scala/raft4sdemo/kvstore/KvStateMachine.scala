package raft4sdemo.kvstore

import cats.effect.IO
import cats.effect.concurrent.Ref
import raft4s.StateMachine
import raft4s.protocol.{ReadCommand, WriteCommand}
import raft4sdemo.kvstore.command.{DeleteCommand, GetCommand, SetCommand}
import raft4sdemo.kvstore.utils.ObjectSerializer

import java.nio.ByteBuffer

class KvStateMachine(lastIndex: Ref[IO, Long], map: Ref[IO, Map[String, String]]) extends StateMachine[IO] {

  override def applyWrite: PartialFunction[(Long, WriteCommand[_]), IO[Any]] = {
    case (index, SetCommand(key, value)) =>
      for {
        _ <- map.update(_ + (key -> value))
        _ <- lastIndex.set(index)
      } yield value

    case (index, DeleteCommand(key)) =>
      for {
        _ <- map.update(_.removed(key))
        _ <- lastIndex.set(index)
      } yield ()
  }

  override def applyRead: PartialFunction[ReadCommand[_], IO[Any]] = { case GetCommand(key) =>
    for {
      items <- map.get
      _ = println(items)
    } yield items(key)
  }

  override def appliedIndex: IO[Long] = lastIndex.get

  override def takeSnapshot(): IO[(Long, ByteBuffer)] =
    for {
      items <- map.get
      index <- lastIndex.get
      bytes = ObjectSerializer.serialize(items)
    } yield (index, bytes)

  override def restoreSnapshot(index: Long, bytes: ByteBuffer): IO[Unit] =
    for {
      _ <- map.set(ObjectSerializer.deserialize(bytes))
      _ <- lastIndex.set(index)
    } yield ()

}

object KvStateMachine {
  def empty: IO[KvStateMachine] =
    for {
      index <- Ref.of[IO, Long](0L)
      map   <- Ref.of[IO, Map[String, String]](Map.empty)
    } yield new KvStateMachine(index, map)
}
