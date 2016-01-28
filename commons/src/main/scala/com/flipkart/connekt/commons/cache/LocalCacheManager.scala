package com.flipkart.connekt.commons.cache

import java.util.concurrent.TimeUnit
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.google.common.cache.{Cache, CacheBuilder, CacheStats}
import scala.collection.{concurrent, Map}
import scala.concurrent.duration.DurationInt

/**
 * Created by nidhi.mehla on 27/01/16.
 */
object LocalCacheManager extends CacheManager {

  var cacheTTLMap: Map[LocalCacheType.Value, CacheProperty] = Map[LocalCacheType.Value, CacheProperty]()
  cacheTTLMap += LocalCacheType.Default -> CacheProperty(100, 1.hour)

  private var cacheStorage = concurrent.TrieMap[LocalCacheType.Value, Caches[AnyRef]]()

  def getCache[V <: Any](cacheName: LocalCacheType.Value)(implicit cTag: reflect.ClassTag[V]): Caches[V] = {
    cacheStorage.get(cacheName) match {
      case Some(x) => x.asInstanceOf[Caches[V]]
      case None =>
        val cache = new LocalCaches[V](cacheName, cacheTTLMap(cacheName))
        cacheStorage += cacheName -> cache.asInstanceOf[Caches[AnyRef]]
        cache
    }
  }

}

class LocalCaches[T](val cacheName: LocalCacheType.Value, props: CacheProperty) extends Caches[T] {

  val cache = CacheBuilder.newBuilder()
    .maximumSize(props.size)
    .expireAfterAccess(LocalCacheManager.cacheTTLMap(cacheName).ttl.toMinutes, TimeUnit.MINUTES)
    .asInstanceOf[CacheBuilder[String, Any]]
    .recordStats()
    .build[String, T]()

  override def put(key: String, value: T): Boolean = {
    try {
      cache.put(key, value)
      true
    } catch {
      case e: Exception => ConnektLogger(LogFile.SERVICE).error("Local cache write failure")
        false
    }
  }

  override def get(key: String): Option[T] = {
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

