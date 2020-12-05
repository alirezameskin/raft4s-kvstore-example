package raft4s.demo.kvstore.command

import raft4s.protocol.WriteCommand

case class SetCommand(key: String, value: String) extends WriteCommand[String]
