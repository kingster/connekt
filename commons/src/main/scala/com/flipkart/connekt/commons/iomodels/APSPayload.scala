package com.flipkart.connekt.commons.iomodels

import java.util.Date

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
case class APSPayloadEnvelope(messageId: String, deviceId: List[String], appName: String, apsPayload: APSPayload)

abstract class APSPayload

case class iOSPNPayload(token: String, expiryInMillis: Long, data: Any) extends APSPayload
