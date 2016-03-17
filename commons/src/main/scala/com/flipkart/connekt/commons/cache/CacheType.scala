/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.cache

trait CacheType extends Enumeration

object DistributedCacheType extends CacheType {
  val Default, AccessTokens, DeviceDetails = Value
}

object LocalCacheType extends CacheType {
  val Default, UserInfo, ResourcePriv, UserConfiguration, WnsAccessToken, Stencils, StencilsBucket, AppCredential = Value
}
