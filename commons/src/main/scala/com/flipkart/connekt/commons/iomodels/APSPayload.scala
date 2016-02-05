package com.flipkart.connekt.commons.iomodels

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
abstract class APSPayload
case class iOSPNPayload(token: String, data: Any) extends APSPayload
