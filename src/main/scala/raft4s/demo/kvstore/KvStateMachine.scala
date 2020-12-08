package raft4s.demo.kvstore

import cats.effect.IO
import raft4s.StateMachine
import raft4s.demo.kvstore.command.{DeleteCommand, GetCommand, SetCommand}
import raft4s.protocol.{ReadCommand, WriteCommand}

import java.util.HashMap

class KvStateMachine extends StateMachine[IO] {
  private var map             = new HashMap[String, String]()
  private var lastIndex: Long = -1

  override def applyWrite: PartialFunction[(Long, WriteCommand[_]), IO[Any]] = {
    case (index, SetCommand(key, value)) =>
      IO {
        map.put(key, value)
        lastIndex = index
        value
      }
    case (index, DeleteCommand(key)) =>
      IO {
        map.remove(key)
        lastIndex = index
        ()
      }
  }

  override def applyRead: PartialFunction[ReadCommand[_], IO[Any]] = { case GetCommand(key) =>
    IO.pure(map.get(key))
  }

  override def appliedIndex: IO[Long] = IO(lastIndex)
}
