package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.cache.{CacheProperty, DistributedCache, DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.tests.CommonsBaseTest

import scala.concurrent.duration.DurationInt


/**
 * Created by nidhi.mehla on 21/01/16.
 */
class CacheTest extends CommonsBaseTest{

  private val keyName: String = UUID.randomUUID().toString
  private val data = "dummy"

  "DistributedCache " should "add " in {
    val cache = new DistributedCache[String](DistributedCacheType.Default, CacheProperty(100, 30.seconds))
     cache.put(keyName, data) shouldEqual true
  }

  "DistributedCache " should "write again " in {
    val cache = new DistributedCache[String](DistributedCacheType.Default, CacheProperty(100, 30.seconds))
    cache.put(keyName, data) shouldEqual true
  }


  "DistributedCache " should "get " in {
    val cache = new DistributedCache[String](DistributedCacheType.Default, CacheProperty(100, 30.seconds))
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

}
