package com.flipkart.connekt.commons.cache

/**
 * Created by nidhi.mehla on 19/01/16.
 */

trait CacheType extends Enumeration

object DistributedCacheType extends CacheType {
  val Default, AccessTokens, DeviceDetails = Value
}

object LocalCacheType extends CacheType {
  val Default, UserAccess, ResourcePriv, UserConfiguration , WNSACCESSTOKEN = Value
}

