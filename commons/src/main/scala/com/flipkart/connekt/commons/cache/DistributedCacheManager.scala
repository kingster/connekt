/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.cache

import com.couchbase.client.java.Bucket
import com.couchbase.client.java.document.StringDocument
import com.couchbase.client.java.error.DocumentDoesNotExistException
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils._
import rx.lang.scala.Observable
import scala.collection.{Map, concurrent}
import scala.concurrent.duration.DurationInt
import scala.reflect.runtime.universe._

object DistributedCacheManager extends CacheManager {

  private var cacheTTLMap: Map[DistributedCacheType.Value, CacheProperty] = Map[DistributedCacheType.Value, CacheProperty]()
  cacheTTLMap += DistributedCacheType.TransientUsers -> CacheProperty(0, 6.hours)
  cacheTTLMap += DistributedCacheType.Default -> CacheProperty(0, 24.hours)
  cacheTTLMap += DistributedCacheType.DeviceDetails -> CacheProperty(0, 0.seconds)

  private var cacheStorage = concurrent.TrieMap[DistributedCacheType.Value, Caches]()

  /**
   * Get Map for given cacheType
   * @param cacheName: cache
   * @tparam V: classType of cache value
   * @return [[Caches]]
   */
  def getCache[V <: Any](cacheName: DistributedCacheType.Value)(implicit cTag: reflect.ClassTag[V]): Caches = {
    cacheStorage.get(cacheName) match {
      case Some(x) => x.asInstanceOf[Caches]
      case None =>
        val cacheStorageBucket = DaoFactory.getCouchbaseBucket(cacheName.toString)
        val cache = new DistributedCaches(cacheName.toString,cacheStorageBucket, cacheTTLMap(cacheName))
        cacheStorage += cacheName -> cache.asInstanceOf[Caches]
        cache
    }
  }

  /**
   * Delete the given key from the distributed cache.
   * @param cacheName: cache Name
   * @param key: cache key
   */
  def delCacheItem(cacheName: DistributedCacheType.Value, key: String): Unit = {
    //DistributedCacheManager.getCache[Any](cacheType).remove(key)
    //    cacheStorage(cacheName).re
    ConnektLogger(LogFile.SERVICE).debug("Cleared Distributed cache " + cacheName.toString + "/" + key)
  }


}

  class DistributedCaches(name:String, cacheStorageBucket:Bucket, props: CacheProperty) extends Caches {


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

  override def put[T](kv: List[(String, T)])(implicit cTag: reflect.ClassTag[T]): Boolean = {
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

  override def get[T](key: String)(implicit cTag: reflect.ClassTag[T]): Option[T] = {
    cacheStorageBucket.get(StringDocument.create(key)) match {
      case null => None
      case x: StringDocument =>
        Option(x.content().getObj[T])
    }
  }

  override def get[T](key: String, tt: TypeTag[T])(implicit tTag: TypeTag[T]): Option[T] = {
    cacheStorageBucket.get(StringDocument.create(key)) match {
      case null => None
      case x: StringDocument =>
        Option(x.content().getObj[T](tt))
    }
  }

  override def remove(key: String) {
    try {
      cacheStorageBucket.remove(StringDocument.create(key))
    } catch {
      case nonExisting: DocumentDoesNotExistException =>
        ConnektLogger(LogFile.SERVICE).warn(s"No Document for $name / $key to Delete")
      case e: Throwable =>
        ConnektLogger(LogFile.SERVICE).error(s"Error removing $key for bucket $name", e)
    }
  }

  override def exists(key: String): Boolean = cacheStorageBucket.get(StringDocument.create(key)) != null

  override def flush(): Unit = {}

  override def get[T](keys: List[String])(implicit cTag: reflect.ClassTag[T]): Predef.Map[String, T] = {
    try {
      Observable.from(keys).flatMap(key => {
        rx.lang.scala.JavaConversions.toScalaObservable(cacheStorageBucket.async().get(StringDocument.create(key))).filter(_ != null).map(d => key -> d.content().getObj[T])
      }).toList.toBlocking.single.toMap
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("DistributedCache multi get Failure", e)
        Predef.Map[String, T]()
    }
  }

  override def get[T](keys: List[String], tt: TypeTag[T])(implicit tTag: TypeTag[T]): Predef.Map[Predef.String, T] = {
    try {
      Observable.from(keys).flatMap(key => {
        rx.lang.scala.JavaConversions.toScalaObservable(cacheStorageBucket.async().get(StringDocument.create(key))).filter(_ != null).map(d => key -> d.content().getObj(tt))
      }).toList.toBlocking.single.toMap
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("DistributedCache multi get Failure", e)
        Predef.Map[String, T]()
    }
  }

}
