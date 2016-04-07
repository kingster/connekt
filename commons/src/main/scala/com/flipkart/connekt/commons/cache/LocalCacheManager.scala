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

import java.util.concurrent.TimeUnit

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.GenericUtils
import com.google.common.cache.{CacheBuilder, CacheStats}

import scala.collection.{Map, concurrent}
import scala.concurrent.duration.DurationInt
import scala.reflect.runtime.universe._

object LocalCacheManager extends CacheManager {

  var cacheTTLMap: Map[LocalCacheType.Value, CacheProperty] = Map[LocalCacheType.Value, CacheProperty]()

  cacheTTLMap += LocalCacheType.Default -> CacheProperty(100, 1.hour)
  cacheTTLMap += LocalCacheType.UserInfo -> CacheProperty(500, 10.hour)
  cacheTTLMap += LocalCacheType.UserGroups -> CacheProperty(1000, 24.hour)
  cacheTTLMap += LocalCacheType.ResourcePriv -> CacheProperty(500, 10.hour)
  cacheTTLMap += LocalCacheType.WnsAccessToken -> CacheProperty(500, 24.hour)
  cacheTTLMap += LocalCacheType.UserConfiguration -> CacheProperty(1000, 24.hour)
  cacheTTLMap += LocalCacheType.Stencils -> CacheProperty(1000, 24.hour)
  cacheTTLMap += LocalCacheType.StencilsBucket -> CacheProperty(100, 24.hour)
  cacheTTLMap += LocalCacheType.AppCredential -> CacheProperty(100, 2.hour)
  cacheTTLMap += LocalCacheType.EngineFabrics -> CacheProperty(1000, 24.hour)
  cacheTTLMap += LocalCacheType.UserGroups -> CacheProperty(500, 10.hour)

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
      case e: Exception => ConnektLogger(LogFile.SERVICE).error("Local cache write failure",e)
        false
    }
  }

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

  override def get[T](keys: List[String])(implicit cTag: reflect.ClassTag[T]): Predef.Map[String, T] = ???

  override def put[T](kv: List[(String, T)])(implicit cTag: reflect.ClassTag[T]): Boolean = ???

  override def get[T](keys: List[String], tt: TypeTag[T])(implicit tTag: TypeTag[T]): Predef.Map[String, T] = {
    get(keys)(GenericUtils.typeToClassTag[T])
  }

  override def get[T](key: String, tt: TypeTag[T])(implicit tTag: TypeTag[T]): Option[T] = {
    get(key)(GenericUtils.typeToClassTag[T])
  }

}
