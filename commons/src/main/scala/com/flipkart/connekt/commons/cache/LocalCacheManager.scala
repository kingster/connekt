package com.flipkart.connekt.commons.cache

import java.util.concurrent.TimeUnit
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.google.common.cache.{Cache, CacheBuilder, CacheStats}
import scala.collection.{concurrent, Map}
import scala.concurrent.duration.DurationInt

/**
 * Created by nidhi.mehla on 27/01/16.
 */
object LocalCacheManager extends CacheManager{

  private var cacheTTLMap: Map[LocalCacheType.Value, CacheProperty] = Map[LocalCacheType.Value, CacheProperty]()
  cacheTTLMap += LocalCacheType.Default -> CacheProperty(100, 1.hour)

  private val caches: concurrent.TrieMap[LocalCacheType.Value, Any] = concurrent.TrieMap[LocalCacheType.Value, Any]()

  private def getCache[V <: Any](cacheType: LocalCacheType.Value): Cache[String, V] =
    caches.getOrElseUpdate(cacheType, {
      CacheBuilder.newBuilder()
        .maximumSize(cacheTTLMap(cacheType).size)
        .expireAfterAccess(cacheTTLMap(cacheType).ttl.toMinutes, TimeUnit.MINUTES)
        .asInstanceOf[CacheBuilder[String, Any]]
        .recordStats()
        .build[String, V]()
    }).asInstanceOf[Cache[String, V]]


  /**
   * Delete all content's for a given local cache
   * @param cacheType
   */
  def clearCache(cacheType: LocalCacheType.Value) {
    getCache(cacheType).cleanUp()
    ConnektLogger(LogFile.SERVICE).info("Cleared local cache" + cacheType.toString)
  }

  /**
   * Delete the given key from the local cache.
   * @param cacheType
   * @param key
   */
  def delCacheItem(cacheType: LocalCacheType.Value)( key: String) {
    getCache[Any](cacheType).invalidate(key)
  }

  /*
   * Insert the given (key,Value) in the local cache.
   * @param cacheType
   * @param key
   * @param value
   */
  def insertCacheItem(cacheType: LocalCacheType.Value)(key: String, value: Any) = {
    getCache(cacheType).put(key, value)
  }

  /*
 * Return the given (key,Value) in the local cache.
 * @param cacheType
 * @param key
 */
  def getCacheItem(cacheType: LocalCacheType.Value)(key: String):Any = {
    getCache(cacheType).getIfPresent(key)
  }

  /**
   * Get Local Cache Stat's
   * @param cacheType
   * @return
   */
  def getStats(cacheType: LocalCacheType.Value): CacheStats = {
    LocalCacheManager.getCache[Any](cacheType).stats()
  }

}

