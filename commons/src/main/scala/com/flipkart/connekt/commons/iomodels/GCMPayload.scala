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

case class GCMPNPayload(registration_ids: Seq[String],
                        @JsonInclude(JsonInclude.Include.NON_NULL) delay_while_idle: Option[Boolean],
                        data: Any,
                        @JsonInclude(JsonInclude.Include.NON_NULL) time_to_live: Option[Long] = None,
                        @JsonInclude(JsonInclude.Include.NON_NULL) dry_run: Option[Boolean] = None) extends GCMPayload

case class GCMPayloadEnvelope(messageId: String, deviceId: Seq[String], appName: String, contextId: String, gcmPayload: GCMPayload, meta: Map[String, Any], headers: Map[String, String] = Map.empty[String, String]) extends PayloadEnvelope
