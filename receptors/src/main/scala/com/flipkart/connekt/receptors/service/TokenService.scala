package com.flipkart.connekt.receptors.service

import java.util.UUID

import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.metrics.Timed

/**
 * Created by avinash.h on 1/27/16.
 */
object TokenService extends Instrumented{

  @Timed("TokenService.get")
  def get(tokenKey: String): Option[String] = {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).get[String](tokenKey)
  }

  @Timed("TokenService.set")
  def set(value: String): Option[String] = {
    val tokenKey = UUID.randomUUID().toString.replaceAll("-", "")
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).put[String](tokenKey, value) match {
      case true =>
        Option(tokenKey)
      case false =>
        None
    }
  }
}
