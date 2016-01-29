package com.flipkart.connekt.receptors.service

import java.util.UUID

import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}

/**
 * Created by avinash.h on 1/27/16.
 */
object TokenService {

  def get(tokenKey: String): Option[String] = {
    DistributedCacheManager.getCache[String](DistributedCacheType.AccessTokens).get(tokenKey)
  }

  def set(value: String): Option[String] = {
    val tokenKey = UUID.randomUUID().toString.replaceAll("-", "")
    DistributedCacheManager.getCache[String](DistributedCacheType.AccessTokens).put(tokenKey, value) match {
      case true =>
        Option(tokenKey)
      case false =>
        None
    }
  }
}
