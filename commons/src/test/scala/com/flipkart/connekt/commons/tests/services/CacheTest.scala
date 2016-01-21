package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.couchbase.client.java.CouchbaseCluster
import com.flipkart.connekt.commons.cache.{DistributedCacheManager, CacheProperty, DistributedCacheType, DistributedCache}
import com.flipkart.connekt.commons.tests.{BaseCommonsTest, ConnektUTSpec}
import scala.concurrent.duration.DurationInt


/**
 * Created by nidhi.mehla on 21/01/16.
 */
class CacheTest extends BaseCommonsTest{

  private val key: String = UUID.randomUUID().toString
  private val data = "dummy"

  "DistributedCache " should "add " in {
    val cache = new DistributedCache[String](DistributedCacheType.Default, CacheProperty(100, 24.hours))
     cache.put(key, data) shouldEqual true
  }

  "DistributedCache " should "write again " in {
    val cache = new DistributedCache[String](DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.put(key, data) shouldEqual true
  }


  "DistributedCache " should "get " in {
    val cache = new DistributedCache[String](DistributedCacheType.Default, CacheProperty(100, 24.hours))
    cache.get(key).isDefined shouldEqual true
    cache.get(key).get shouldEqual data
  }

  "DistributedCacheManager" should "get" in {
    DistributedCacheManager.getCache[String](DistributedCacheType.Default).get(key).get shouldEqual data
  }

  "DistributedCacheManager" should "write different" in {
    DistributedCacheManager.getCache[String](DistributedCacheType.AccessTokens).put(key, "i-dont-care") shouldEqual true
  }


  "DistributedCacheManager" should "get different" in {
    DistributedCacheManager.getCache[String](DistributedCacheType.Default).get(key).get shouldEqual data
  }



}
