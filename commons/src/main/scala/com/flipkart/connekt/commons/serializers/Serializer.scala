/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.serializers

trait Serializer {

  def serialize(obj: AnyRef): Array[Byte]

  def deserialize[T](bytes: Array[Byte])(implicit cTag: reflect.ClassTag[T]): T
}
