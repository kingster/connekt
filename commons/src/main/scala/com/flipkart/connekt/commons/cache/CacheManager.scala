package com.flipkart.connekt.commons.cache

import scala.concurrent.duration.Duration

/**
 * Created by nidhi.mehla on 19/01/16.
 */
trait CacheManager {

}

abstract class Caches[T] {
  def put(key: String, value: T): Boolean

  def get(key: String): Option[T]

  def exists(key: String): Boolean

  def flush(): Unit
}

case class CacheProperty(size: Int, ttl: Duration)