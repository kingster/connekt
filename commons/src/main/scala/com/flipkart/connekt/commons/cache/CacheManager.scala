package com.flipkart.connekt.commons.cache

import scala.concurrent.duration.Duration

/**
 * Created by nidhi.mehla on 19/01/16.
 */
trait CacheManager {

}

abstract class Caches {

  def put[T](key: String, value: T)(implicit cTag: reflect.ClassTag[T]): Boolean

  def multiPut[T](kv: scala.collection.immutable.Map[String, T])(implicit cTag: reflect.ClassTag[T]): Boolean

  def get[T](key: String)(implicit cTag: reflect.ClassTag[T]): Option[T]

  def remove(key:String):Unit

  def exists(key: String): Boolean

  def flush(): Unit
}

case class CacheProperty(size: Int, ttl: Duration)