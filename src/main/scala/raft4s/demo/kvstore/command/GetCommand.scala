package raft4s.demo.kvstore.command

import raft4s.protocol.ReadCommand

case class GetCommand(key: String) extends ReadCommand[String]
