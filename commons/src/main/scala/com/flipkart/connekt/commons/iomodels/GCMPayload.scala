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

abstract class GCMPayload

abstract class PayloadEnvelope

trait OpenWebPayload

case class GCMPNPayload(registration_ids: Seq[String],
                        @JsonInclude(JsonInclude.Include.NON_NULL) delay_while_idle: Option[Boolean],
                        data: Any,
                        @JsonInclude(JsonInclude.Include.NON_NULL) time_to_live: Option[Long] = None,
                        @JsonInclude(JsonInclude.Include.NON_NULL) dry_run: Option[Boolean] = None) extends GCMPayload

case class OpenWebGCMPayload(registration_ids: Seq[String],
                             @JsonInclude(JsonInclude.Include.NON_NULL) raw_data: Option[String] = None,
                             @JsonInclude(JsonInclude.Include.NON_NULL) dry_run: Option[Boolean] = None) extends GCMPayload with OpenWebPayload

case class GCMPayloadEnvelope(messageId: String, deviceId: Seq[String], appName: String, contextId:String, gcmPayload: GCMPayload) extends PayloadEnvelope

case class OpenWebPayloadEnvelope(messageId: String, deviceId: Seq[String], appName: String, contextId:String, provider:String, payload: OpenWebPayload)  extends PayloadEnvelope
