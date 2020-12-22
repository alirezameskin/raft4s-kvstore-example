package raft4sdemo.kvstore

import raft4s.{ReadCommand, WriteCommand}

case class GetCommand(key: String) extends ReadCommand[String]
case class DeleteCommand(key: String) extends WriteCommand[Unit]
case class SetCommand(key: String, value: String) extends WriteCommand[String]
