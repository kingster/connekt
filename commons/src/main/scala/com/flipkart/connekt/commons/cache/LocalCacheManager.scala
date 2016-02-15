package com.flipkart.connekt.commons.cache

import java.util.concurrent.TimeUnit

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.google.common.cache.{CacheBuilder, CacheStats}

import scala.collection.{Map, concurrent}
import scala.concurrent.duration.DurationInt

/**
 * Created by nidhi.mehla on 27/01/16.
 */
object LocalCacheManager extends CacheManager {

  var cacheTTLMap: Map[LocalCacheType.Value, CacheProperty] = Map[LocalCacheType.Value, CacheProperty]()

  cacheTTLMap += LocalCacheType.Default -> CacheProperty(100, 1.hour)
  cacheTTLMap += LocalCacheType.ResourcePriv -> CacheProperty(500, 10.hour)
  cacheTTLMap += LocalCacheType.UserConfiguration -> CacheProperty(1000, 24.hour)

  private var cacheStorage = concurrent.TrieMap[LocalCacheType.Value, Caches]()

  def getCache[V <: Any](cacheName: LocalCacheType.Value)(implicit cTag: reflect.ClassTag[V]): Caches = {
    cacheStorage.get(cacheName) match {
      case Some(x) => x.asInstanceOf[Caches]
      case None =>
        val cache = new LocalCaches(cacheName, cacheTTLMap(cacheName))
        cacheStorage += cacheName -> cache.asInstanceOf[Caches]
        cache
    }
  }

}

class LocalCaches(val cacheName: LocalCacheType.Value, props: CacheProperty) extends Caches {

  val cache = CacheBuilder.newBuilder()
    .maximumSize(props.size)
    .expireAfterAccess(LocalCacheManager.cacheTTLMap(cacheName).ttl.toMinutes, TimeUnit.MINUTES)
    .asInstanceOf[CacheBuilder[String, Any]]
    .recordStats()
    .build[String, AnyRef]()

  override def put[T](key: String, value: T)(implicit cTag: reflect.ClassTag[T]): Boolean = {
    try {
      cache.put(key, value.asInstanceOf[AnyRef])
      true
    } catch {
      case e: Exception => ConnektLogger(LogFile.SERVICE).error("Local cache write failure")
        false
    }
  }

  override def multiPut[T](map: Predef.Map[String, T])(implicit cTag: ClassManifest[T]): Boolean = ???

  override def get[T](key: String)(implicit cTag: reflect.ClassTag[T]): Option[T] = {
    cache.getIfPresent(key) match {
      case x: Any => Option(x.asInstanceOf[T])
      case _ => None
    }
  }


  override def exists(key: String): Boolean = get(key).isDefined


  override def flush() {
    cache.cleanUp()
    ConnektLogger(LogFile.SERVICE).info("Cleared local cache" + cacheName.toString)
  }

  /**
   * Get Local Cache Stat's
   * @return
   */
  def getStats: CacheStats = {
    cache.stats()
  }

  /**
   * Delete the given key from the local cache.
   * @param key
   */
  def remove(key: String) {
    cache.invalidate(key)
  }
}

