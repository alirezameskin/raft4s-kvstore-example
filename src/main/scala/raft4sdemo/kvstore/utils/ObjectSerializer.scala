package raft4sdemo.kvstore.utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer

object ObjectSerializer {

  def serialize(items: Map[String, String]): ByteBuffer = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos                           = new ObjectOutputStream(stream)
    oos.writeObject(items)
    oos.close

    ByteBuffer.wrap(stream.toByteArray)
  }

  def deserialize(bytes: ByteBuffer): Map[String, String] = {

    val ois      = new ObjectInputStream(new ByteArrayInputStream(bytes.array()))
    val response = ois.readObject().asInstanceOf[Map[String, String]]
    ois.close()

    response
  }
}
