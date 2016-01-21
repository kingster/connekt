package com.flipkart.connekt.commons.cache

import com.couchbase.client.java.document.JsonDocument
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

  private var cacheStorage: Map[DistributedCacheType.Value, Cache[AnyRef]] = Map()

  /**
   * Get Map for given cacheType
   * @param cacheName
   * @tparam V
   * @return [[Cache]]
   */
  def getCache[V <: Any](cacheName: DistributedCacheType.Value)(implicit cTag: reflect.ClassTag[V]): Cache[V] = {
    cacheStorage.get(cacheName) match {
      case Some(x) => x.asInstanceOf[Cache[V]]
      case None =>
        val cache = new DistributedCache[V](cacheName, cacheTTLMap(cacheName))
        cacheStorage += cacheName -> cache.asInstanceOf[Cache[AnyRef]]
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

class DistributedCache[T](val cacheName: DistributedCacheType.Value, props: CacheProperty)(implicit cTag: reflect.ClassTag[T]) extends Cache[T] {

  private lazy val cacheStorageBucket = DaoFactory.getCouchbaseBucket()

  override def put(key: String, value: T): Boolean = {
    try {
      val data = Map("d" -> value.asInstanceOf[AnyRef].getJson).asJava
      cacheStorageBucket.insert(JsonDocument.create(key, JsonObject.from(data)))
      true
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("DistributedCache Write Failure", e)
        false
    }
  }

  override def get(key: String): Option[T] = {
    cacheStorageBucket.get(key) match {
      case null => None
      case x: JsonDocument =>
        val actualData = x.content().get("d")
        Option(actualData.toString.getObj[T])
    }
  }

  def remove(key: String) {
    cacheStorageBucket.remove(key)
  }

  override def exists(key: String): Boolean = ???
}