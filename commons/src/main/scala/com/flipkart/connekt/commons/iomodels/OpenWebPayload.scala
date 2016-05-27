/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonInclude

trait OpenWebPayload

case class OpenWebChromePayload(registration_ids: Seq[String],
                                @JsonInclude(JsonInclude.Include.NON_NULL) raw_data: String,
                                @JsonInclude(JsonInclude.Include.NON_NULL) time_to_live: Option[Long] = None,
                                @JsonInclude(JsonInclude.Include.NON_NULL) dry_run: Option[Boolean] = None) extends GCMPayload with OpenWebPayload

case class OpenWebStandardPayload(data:Array[Byte]) extends OpenWebPayload

case class OpenWebStandardPayloadEnvelope(messageId: String, deviceId: String, appName: String, contextId: String, providerUrl:String, payload: OpenWebStandardPayload, headers: Map[String, String], meta: Map[String, Any]) extends PayloadEnvelope
