package com.flipkart.connekt.commons.cache

import com.couchbase.client.java.document.StringDocument
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils._
import rx.lang.scala.Observable

import scala.Predef
import scala.collection.{Map, concurrent}
import scala.concurrent.duration.DurationInt

/**
 * Created by nidhi.mehla on 19/01/16.
 */
object DistributedCacheManager extends CacheManager {

  private var cacheTTLMap: Map[DistributedCacheType.Value, CacheProperty] = Map[DistributedCacheType.Value, CacheProperty]()
  cacheTTLMap += DistributedCacheType.AccessTokens -> CacheProperty(5000, 6.hours)
  cacheTTLMap += DistributedCacheType.Default -> CacheProperty(100, 24.hours)
  cacheTTLMap += DistributedCacheType.DeviceDetails -> CacheProperty(100, 24.hours)

  private var cacheStorage = concurrent.TrieMap[DistributedCacheType.Value, Caches]()

  /**
   * Get Map for given cacheType
   * @param cacheName
   * @tparam V
   * @return [[Caches]]
   */
  def getCache[V <: Any](cacheName: DistributedCacheType.Value)(implicit cTag: reflect.ClassTag[V]): Caches = {
    cacheStorage.get(cacheName) match {
      case Some(x) => x.asInstanceOf[Caches]
      case None =>
        val cache = new DistributedCaches(cacheName, cacheTTLMap(cacheName))
        cacheStorage += cacheName -> cache.asInstanceOf[Caches]
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

class DistributedCaches(val cacheName: DistributedCacheType.Value, props: CacheProperty) extends Caches {

  private lazy val cacheStorageBucket = DaoFactory.getCouchbaseBucket(cacheName.toString)

  override def put[T](key: String, value: T)(implicit cTag: reflect.ClassTag[T]): Boolean = {
    try {
      cacheStorageBucket.upsert(StringDocument.create(key, props.ttl.toSeconds.toInt, value.asInstanceOf[AnyRef].getJson))
      true
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("DistributedCache Write Failure", e)
        false
    }
  }

  override def multiPut[T](kvMap: scala.collection.immutable.Map[String, T])(implicit cTag: ClassManifest[T]): Boolean = {
    try {
      var done = true
      for ((k, v) <- kvMap) {
        put(k, v) && done
      }
      done
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("DistributedCache Write Failure", e)
        false
    }
  }


  def put[T](kv: List[(String, T)])(implicit cTag: reflect.ClassTag[T]): Boolean = {
    try {
      val documents = kv.map(doc => StringDocument.create(doc._1, props.ttl.toSeconds.toInt, doc._2.asInstanceOf[AnyRef].getJson))
      Observable.from(documents).flatMap(doc => {
        rx.lang.scala.JavaConversions.toScalaObservable(cacheStorageBucket.async().upsert(doc))
      }).last.toBlocking.single

      true
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("DistributedCache Write Failure", e)
        false
    }
  }

  def get[T](key: String)(implicit cTag: reflect.ClassTag[T]): Option[T] = {
    cacheStorageBucket.get(StringDocument.create(key)) match {
      case null => None
      case x: StringDocument =>
        Option(x.content().getObj[T])
    }
  }

  def remove(key: String) {
    try {
      cacheStorageBucket.remove(StringDocument.create(key))
    } catch {
      case e: Exception =>
    }
  }

  override def exists(key: String): Boolean = cacheStorageBucket.get(StringDocument.create(key)) != null

  override def flush(): Unit = ???
}