/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.serializers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.pool.{KryoFactory, KryoPool}

object KryoSerializer extends Serializer {

  val factory = new KryoFactory() {
    override def create(): Kryo = {
      val kryo = new Kryo()
      // configure kryo instance, customize settings
      kryo
    }
  }

  /**
   * Why pool ? Kryo is not thread safe. Each thread should have its own Kryo, Input, and Output instances.
   * Also, the byte[] Input uses may be modified and then returned to its original state during deserialization,
   * so the same byte[] "should not be used concurrently in separate threads.
   *
   * One of the suggestion is to use kryo pool
   */
  val kryoPool = new KryoPool.Builder(factory).softReferences().build()


  override def serialize(obj: AnyRef): Array[Byte] = {

    val stream = new ByteArrayOutputStream()
    val output = new Output(stream)
    val kryoInstance = kryoPool.borrow()
    kryoInstance.writeClassAndObject(output, obj)
    output.close()
    kryoPool.release(kryoInstance)
    stream.toByteArray
  }

  override def deserialize[T](bytes: Array[Byte])(implicit cTag: reflect.ClassTag[T]): T = {
    val stream = new ByteArrayInputStream(bytes)
    val input = new Input(stream)
    val kryoInstance = kryoPool.borrow()
    val obj = kryoInstance.readClassAndObject(input)
    kryoPool.release(kryoInstance)
    obj.asInstanceOf[T]
  }
}
