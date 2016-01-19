package com.flipkart.connekt.commons.entities

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
object Channel extends Enumeration {
  type Channel = Value
  val PUSH = Value("push")
  val EMAIL = Value("email")
  val SMS = Value("sms")
  val CARDS = Value("cards")
  val OPENWEB = Value("openweb")
}
