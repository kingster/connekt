package com.flipkart.connekt.commons.cache

import com.couchbase.client.java.document.{StringDocument, JsonDocument}
import com.couchbase.client.java.document.json.JsonObject
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.utils.StringUtils

import scala.collection.Map
import scala.concurrent.duration.DurationInt
import com.flipkart.connekt.commons.utils.StringUtils._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * Created by nidhi.mehla on 19/01/16.
 */
object DistributedCacheManager extends CacheManager {

  private var cacheTTLMap: Map[DistributedCacheType.Value, CacheProperty] = Map[DistributedCacheType.Value, CacheProperty]()
  cacheTTLMap += DistributedCacheType.AccessTokens -> CacheProperty(5000, 24.hours)
  cacheTTLMap += DistributedCacheType.Default -> CacheProperty(100, 24.hours)

  private var cacheStorage: Map[DistributedCacheType.Value, Caches[AnyRef]] = Map()

  /**
   * Get Map for given cacheType
   * @param cacheName
   * @tparam V
   * @return [[Caches]]
   */
  def getCache[V <: Any](cacheName: DistributedCacheType.Value)(implicit cTag: reflect.ClassTag[V]): Caches[V] = {
    cacheStorage.get(cacheName) match {
      case Some(x) => x.asInstanceOf[Caches[V]]
      case None =>
        val cache = new DistributedCaches[V](cacheName, cacheTTLMap(cacheName))
        cacheStorage += cacheName -> cache.asInstanceOf[Caches[AnyRef]]
        cache
    }
  }

  /**
   * Delete the given key from the distributed cache.
   * @param cacheName
   * @param key
   */
  def delCacheItem(cacheName: DistributedCacheType.Value, key: String): Unit = {
    //DistributedCacheManager.getCache[Any](cacheType).remove(key)
    //    cacheStorage(cacheName).re
    ConnektLogger(LogFile.SERVICE).debug("Cleared Distributed cache " + cacheName.toString + "/" + key)
  }


}

class DistributedCaches[T](val cacheName: DistributedCacheType.Value, props: CacheProperty)(implicit cTag: reflect.ClassTag[T]) extends Caches[T] {

  private lazy val cacheStorageBucket = DaoFactory.getCouchbaseBucket(cacheName.toString)

  override def put(key: String, value: T): Boolean = {
    try {
      cacheStorageBucket.upsert(StringDocument.create(key, value.asInstanceOf[AnyRef].getJson))
      true
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("DistributedCache Write Failure", e)
        false
    }
  }

  override def get(key: String): Option[T] = {
    cacheStorageBucket.get(StringDocument.create(key)) match {
      case null => None
      case x: StringDocument =>
        Option(x.content().getObj[T])
    }
  }

  def remove(key: String) {
    cacheStorageBucket.remove(StringDocument.create(key))
  }

  override def exists(key: String): Boolean = ???
}