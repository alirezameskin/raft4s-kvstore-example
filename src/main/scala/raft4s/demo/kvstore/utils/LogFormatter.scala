package raft4s.demo.kvstore.utils

import io.odin.LoggerMessage
import io.odin.formatter.Formatter
import io.odin.formatter.Formatter.{formatThrowable, formatTimestamp}
import io.odin.formatter.options.ThrowableFormat
import perfolation._

class LogFormatter extends Formatter {

  override def format(msg: LoggerMessage): String = {
    val timestamp = formatTimestamp(msg.timestamp)
    val level     = msg.level.toString

    val throwable = msg.exception match {
      case Some(t) =>
        p"${System.lineSeparator()}${formatThrowable(t, ThrowableFormat.Default)}"
      case None =>
        ""
    }

    p"$timestamp $level  - ${msg.message.value}$throwable"
  }
}
