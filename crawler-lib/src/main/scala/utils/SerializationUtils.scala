package org.blikk.crawler

import java.io._

object SerializationUtils {

  def serialize(obj: Any) : Array[Byte] = {
    val bos = new ByteArrayOutputStream() 
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(obj)
    oos.close()
    bos.toByteArray
  }

  def deserialize[A](bytes: Array[Byte]) ={
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis)
    ois.readObject().asInstanceOf[A]
  }

}