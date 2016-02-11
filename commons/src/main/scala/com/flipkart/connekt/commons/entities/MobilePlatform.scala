package com.flipkart.connekt.commons.entities

/**
 *
 *
 * @author durga.s
 * @version 2/11/16
 */
object MobilePlatform extends Enumeration {
  type MobilePlatform = Value

  val ANDROID = Value("android")
  val IOS = Value("ios")
  val WINDOWS = Value("windows")
  val UNKNOWN = Value("unknown")
}
