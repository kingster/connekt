/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities

object MobilePlatform extends Enumeration {
  type MobilePlatform = Value

  val ANDROID = Value("android")
  val IOS = Value("ios")
  val WINDOWS = Value("windows")
  val UNKNOWN = Value("unknown")
}
