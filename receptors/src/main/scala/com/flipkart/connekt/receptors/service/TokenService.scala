package com.flipkart.connekt.receptors.service

import java.util.UUID

import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.metrics.Timed
import com.flipkart.connekt.commons.core.Wrappers._

import scala.util.Try

/**
 * Created by avinash.h on 1/27/16.
 */
object TokenService extends Instrumented {

  @Timed("get")
  def get(tokenKey: String): Try[Option[String]] = Try_ {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).get[String](tokenKey)
  }

  @Timed("set")
  def set(value: String):  Try[Option[String]] = Try_ {
    val tokenKey = UUID.randomUUID().toString.replaceAll("-", "")
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).put[String](tokenKey, value) match {
      case true =>
        Option(tokenKey)
      case false =>
        None
    }
  }
}
