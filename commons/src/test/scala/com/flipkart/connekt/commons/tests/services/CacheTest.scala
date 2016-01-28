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
    val cache = new DistributedCaches[String](DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.put(keyName, data) shouldEqual true
  }

  "DistributedCache " should "write again " in {
    val cache = new DistributedCaches[String](DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.put(keyName, data) shouldEqual true
  }

  "DistributedCache " should "get " in {
    val cache = new DistributedCaches[String](DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.get(keyName).isDefined shouldEqual true
    cache.get(keyName).get shouldEqual data
  }

  "DistributedCacheManager" should "get" in {
    DistributedCacheManager.getCache[String](DistributedCacheType.Default).get(keyName).get shouldEqual data
  }

  "DistributedCacheManager" should "write different" in {
    DistributedCacheManager.getCache[String](DistributedCacheType.AccessTokens).put(keyName, "i-dont-care") shouldEqual true
  }

  "DistributedCacheManager" should "get different" in {
    DistributedCacheManager.getCache[String](DistributedCacheType.Default).get(keyName).get shouldEqual data
  }

  "LocalCacheManager" should "insert" in {
    LocalCacheManager.getCache[String](LocalCacheType.Default).put(keyName, data)
  }

  "LocalCacheManager" should "get" in {
    LocalCacheManager.getCache[String](LocalCacheType.Default).get(keyName).get shouldEqual data
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
