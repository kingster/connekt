/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

case class APSPayloadEnvelope(messageId: String, deviceId: String, appName: String, apsPayload: APSPayload)

abstract class APSPayload
case class iOSPNPayload(token: String, expiryInMillis: Long, data: Any) extends APSPayload
