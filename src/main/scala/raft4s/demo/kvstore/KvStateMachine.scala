package raft4s.demo.kvstore

import cats.effect.IO
import cats.effect.concurrent.Ref
import raft4s.StateMachine
import raft4s.demo.kvstore.command.{DeleteCommand, GetCommand, SetCommand}
import raft4s.protocol.{ReadCommand, WriteCommand}
import raft4s.storage.Snapshot

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
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

  override def takeSnapshot(): IO[Snapshot] =
    for {
      items <- map.get
      index <- lastIndex.get
      bytes = serialize(items)
    } yield Snapshot(index, bytes)

  override def restoreSnapshot(snapshot: Snapshot): IO[Unit] =
    for {
      _ <- map.set(deserialize(snapshot.bytes))
      _ <- lastIndex.set(snapshot.lastIndex)
    } yield ()

  private def serialize(items: Map[String, String]): ByteBuffer = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos                           = new ObjectOutputStream(stream)
    oos.writeObject(items)
    oos.close

    ByteBuffer.wrap(stream.toByteArray)
  }

  private def deserialize(bytes: ByteBuffer): Map[String, String] = {

    val ois      = new ObjectInputStream(new ByteArrayInputStream(bytes.array()))
    val response = ois.readObject().asInstanceOf[Map[String, String]]
    ois.close()

    response
  }
}

object KvStateMachine {
  def empty: IO[KvStateMachine] =
    for {
      index <- Ref.of[IO, Long](-1L)
      map   <- Ref.of[IO, Map[String, String]](Map.empty)
    } yield new KvStateMachine(index, map)
}
