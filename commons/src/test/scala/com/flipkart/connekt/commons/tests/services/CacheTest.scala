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

  "LocalCacheManager" should "write" in {
    LocalCacheManager.insertCacheItem(LocalCacheType.Default)(keyName, value)
  }

  "LocalCacheManager" should "get" in {
    LocalCacheManager.getCacheItem(LocalCacheType.Default)(keyName) shouldEqual value
  }

  "LocalCacheManager" should "delete" in {
    LocalCacheManager.delCacheItem(LocalCacheType.Default)(keyName)
  }

}
