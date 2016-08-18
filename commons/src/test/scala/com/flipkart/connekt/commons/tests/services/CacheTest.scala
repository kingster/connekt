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
package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.cache._
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.tests.CommonsBaseTest

import scala.concurrent.duration.DurationInt


class CacheTest extends CommonsBaseTest {

  private val keyName: String = UUID.randomUUID().toString
  private val data = "dummy"

  "DistributedCache " should "add " in {
    val cache = new DistributedCaches(DistributedCacheType.Default.toString,DaoFactory.getCouchbaseBucket(DistributedCacheType.Default.toString), CacheProperty(100, 24.hours))
    cache.put[String](keyName, data) shouldEqual true
  }

  "DistributedCache " should "write again " in {
    val cache = new DistributedCaches(DistributedCacheType.Default.toString,DaoFactory.getCouchbaseBucket(DistributedCacheType.Default.toString), CacheProperty(100, 24.hours))
    cache.put[String](keyName, data) shouldEqual true
  }


  "DistributedCache " should "get " in {
    val cache = new DistributedCaches(DistributedCacheType.Default.toString,DaoFactory.getCouchbaseBucket(DistributedCacheType.Default.toString), CacheProperty(100, 24.hours))
    cache.get[String](keyName).isDefined shouldEqual true
    cache.get[String](keyName).get shouldEqual data
  }

  "DistributedCache " should "bulk write " in {
    val cache = new DistributedCaches(DistributedCacheType.Default.toString,DaoFactory.getCouchbaseBucket(DistributedCacheType.Default.toString), CacheProperty(100, 24.hours))
    cache.put[String](List(Tuple2("k1", data),Tuple2("k2", data),Tuple2("k3", data), Tuple2("k4", data))) shouldEqual true
  }

  "DistributedCache " should "get from bulk write " in {
    val cache = new DistributedCaches(DistributedCacheType.Default.toString,DaoFactory.getCouchbaseBucket(DistributedCacheType.Default.toString), CacheProperty(100, 24.hours))
    cache.get[String]("k2").isDefined shouldEqual true
    cache.get[String]("k2").get shouldEqual data
  }

  "DistributedCacheManager" should "get" in {
    DistributedCacheManager.getCache(DistributedCacheType.Default).get[String](keyName).get shouldEqual data
  }

  "DistributedCacheManager" should "write different" in {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).put[AppUser](keyName, new AppUser("userId", "apiKey", "groups", "contact")) shouldEqual true
  }

  "DistributedCacheManager" should "write different type" in {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).put[Int]("int", 1) shouldEqual true
  }

  "DistributedCacheManager" should "get different type" in {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).get[Int]("int").get shouldEqual 1
  }

  "DistributedCacheManager" should "get different" in {
    DistributedCacheManager.getCache(DistributedCacheType.Default).get[String](keyName).get shouldEqual data
  }

  "DistributedCacheManager" should "write different list" in {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).put[List[String]]("list-string", List("a", "b")) shouldEqual true
  }

  "DistributedCacheManager" should "get different list" in {
    val d = DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).get[List[String]]("list-string").get
    d shouldEqual List("a", "b")
  }

  "Distributed CacheManger" should "write in batch" in {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).put[String](List("a" -> "a", "b" ->"b")) shouldEqual true
    val result = DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).get[String](List("a", "b"))
    result.size shouldEqual 2
    result("a") shouldEqual "a"
  }

  "Distributed CacheManager" should "delete element" in {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).put[String]("x", "y")
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).remove("x")

    val result = DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).get[String]("x")
    println("result = " + result)

    result shouldEqual None

  }

  "LocalCacheManager" should "insert" in {
    LocalCacheManager.getCache(LocalCacheType.Default).put(keyName, data)
  }

  "LocalCacheManager" should "get" in {
    LocalCacheManager.getCache(LocalCacheType.Default).get[String](keyName).get shouldEqual data
  }

}
