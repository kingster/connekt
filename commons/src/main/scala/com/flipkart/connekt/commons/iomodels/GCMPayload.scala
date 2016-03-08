package com.flipkart.connekt.commons.iomodels

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
case class GCMPayloadEnvelope(messageId: String, deviceId: List[String], appName: String, gcmPayload: GCMPayload)

abstract class GCMPayload
case class GCMPNPayload(registration_ids: List[String], delay_while_idle: Boolean, data: Any) extends GCMPayload
case class OpenWebGCMPayload(registration_ids: List[String]) extends GCMPayload