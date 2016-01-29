package com.flipkart.connekt.receptors.service

import java.util.UUID

import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}

/**
 * Created by avinash.h on 1/27/16.
 */
object TokenService {

  def get(tokenKey: String): Option[String] = {
    DistributedCacheManager.getCache(DistributedCacheType.AccessTokens).get[String](tokenKey)
  }

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
