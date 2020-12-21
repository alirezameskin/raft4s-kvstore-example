package raft4sdemo.kvstore.command

import raft4s.protocol.WriteCommand

case class DeleteCommand(key: String) extends WriteCommand[Unit]
