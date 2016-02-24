package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.cache._
import com.flipkart.connekt.commons.tests.CommonsBaseTest

import scala.concurrent.duration.DurationInt


/**
 * Created by nidhi.mehla on 21/01/16.
 */

class CacheTest extends CommonsBaseTest {

  private val keyName: String = UUID.randomUUID().toString
  private val data = "dummy"

  "DistributedCache " should "add " in {
    val cache = new DistributedCaches(DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.put[String](keyName, data) shouldEqual true
  }

  "DistributedCache " should "write again " in {
    val cache = new DistributedCaches(DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.put[String](keyName, data) shouldEqual true
  }


  "DistributedCache " should "get " in {
    val cache = new DistributedCaches(DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.get[String](keyName).isDefined shouldEqual true
    cache.get[String](keyName).get shouldEqual data
  }

  "DistributedCache " should "bulk write " in {
    val cache = new DistributedCaches(DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.put[String](List(Tuple2("k1", data),Tuple2("k2", data),Tuple2("k3", data), Tuple2("k4", data))) shouldEqual true
  }

  "DistributedCache " should "get from bulk write " in {
    val cache = new DistributedCaches(DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.get[String]("k2").isDefined shouldEqual true
    cache.get[String]("k2").get shouldEqual data
  }


  "DistributedCacheManager" should "get" in {
    DistributedCacheManager.getCache(DistributedCacheType.Default).get[String](keyName).get shouldEqual data
  }

  "DistributedCacheManager" should "write different" in {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).put[String](keyName, "i-dont-care") shouldEqual true
  }

  "DistributedCacheManager" should "write different type" in {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).put[Int]("int",1) shouldEqual true
  }

  "DistributedCacheManager" should "get different type" in {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).get[Int]("int").get shouldEqual 1
  }

  "DistributedCacheManager" should "get different" in {
    DistributedCacheManager.getCache(DistributedCacheType.Default).get[String](keyName).get shouldEqual data
  }

  "DistributedCacheManager" should "write different list" in {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).put[List[String]]("list-string", List("a", "b")) shouldEqual true
  }

  "DistributedCacheManager" should "get different list" in {
    val d = DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).get[List[String]]("list-string").get
    d shouldEqual List("a", "b")
  }

  "Distributed CacheManger" should "write in batch" in {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).put[String](List("a" -> "a", "b" ->"b")) shouldEqual true
    val result = DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).get[String](List("a", "b"))
    result.size shouldEqual 2
    result("a") shouldEqual "a"
  }

  "LocalCacheManager" should "insert" in {
    LocalCacheManager.getCache(LocalCacheType.Default).put(keyName, data)
  }

  "LocalCacheManager" should "get" in {
    LocalCacheManager.getCache(LocalCacheType.Default).get[String](keyName).get shouldEqual data
  }

//
//  "LocalCacheManager" should "insert null" in {
//    LocalCacheManager.getCache[String](LocalCacheType.Default).put("null", null) shouldEqual true
//  }
//
//  "LocalCacheManager" should "get null" in {
//    LocalCacheManager.getCache[String](LocalCacheType.Default).get("null").isDefined shouldEqual false
//  }

}
