package com.flipkart.connekt.commons.serializers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}

/**
 * Created by kinshuk.bairagi on 19/02/16.
 */
object KryoSerializer extends Serializer{

  val kryoInstance = new Kryo()

  override def serialize(obj: AnyRef): Array[Byte] = {
    val stream = new ByteArrayOutputStream()
    val output = new Output(stream)
    kryoInstance.writeClassAndObject(output, obj)
    output.close()
    stream.toByteArray
  }

  override def deserialize[T](bytes: Array[Byte])(implicit cTag: reflect.ClassTag[T]): T = {
    val stream = new ByteArrayInputStream(bytes)
    val input = new Input(stream)
    val obj = kryoInstance.readClassAndObject(input)
    obj.asInstanceOf[T]
  }
}
