package com.flipkart.connekt.commons.serializers

/**
 * Created by kinshuk.bairagi on 19/02/16.
 */
trait Serializer {

  def serialize(obj: AnyRef): Array[Byte]

  def deserialize[T](bytes: Array[Byte])(implicit cTag: reflect.ClassTag[T]): T
}
