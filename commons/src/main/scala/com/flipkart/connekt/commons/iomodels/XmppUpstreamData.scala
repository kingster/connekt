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

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import com.flipkart.connekt.commons.core.Wrappers._

@JsonIgnoreProperties(ignoreUnknown = true)
case class XmppUpstreamData(
                             @JsonProperty("message_id") @JsonProperty(required = true) messageId: String,
                             @JsonProperty(required = true) from: String,
                             @JsonProperty(required = true) category: String,
                             @JsonProperty(required = true) data: ObjectNode) extends XmppUpstreamResponse(messageId, from, category) {

  override def getPnCallbackEvent(): Option[PNCallbackEvent] = Try_ {
    val eventType: JsonNode = data.get("eventType")
    val appName = Option(data.get("appName")).map(_.asText()).getOrElse("NA")
    val deviceId = Option(data.get("deviceId")).map(_.asText()).orElse(DeviceDetailsService.getByTokenId(from, appName).get.map(_.deviceId))
    if (eventType != null && deviceId.nonEmpty) {
      Some(PNCallbackEvent(
        messageId =  Option(data.get("messageId")).map(_.asText(messageId)).getOrElse(messageId),
        clientId = category.split('.').last,
        deviceId = deviceId.get,
        eventType = eventType.asText(),
        platform = MobilePlatform.ANDROID,
        appName = appName,
        contextId = Option(data.get("contextId")).map(_.asText()).orEmpty,
        cargo = Option(data.get("cargo")).map(_.asText()).orNull,
        timestamp = Option(data.get("timestamp")).map(_.asText().toLong).getOrElse(System.currentTimeMillis)
      ))
    } else {
      ConnektLogger(LogFile.PROCESSORS).warn("Dropped XmppUpstreamData : " + data)
      None
    }
  }.getOrElse(None)
}

/** Sample from GCM
  * <message id="">
  * <gcm xmlns="google:mobile:data">
  * {
  * "category":"com.example.yourapp", // to know which app sent it
  * "data":
  * {
  * "hello":"world",
  * },
  * "message_id":"m-123",
  * "from":"REGID"
  * }
  * </gcm>
  * </message>
  */
