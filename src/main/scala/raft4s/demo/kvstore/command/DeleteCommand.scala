package raft4s.demo.kvstore.command

import raft4s.protocol.WriteCommand

case class DeleteCommand(key: String) extends WriteCommand[Unit]
