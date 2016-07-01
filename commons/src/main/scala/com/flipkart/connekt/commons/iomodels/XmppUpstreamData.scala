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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Created by subir.dey on 23/06/16.
 */
case class XmppUpstreamData (
                              @JsonProperty("message_id")@JsonProperty(required = true) messageId: String,
                              @JsonProperty(required = true) from: String,
                              @JsonProperty(required = true) category: String,
                              @JsonProperty(required = false) data: ObjectNode) extends XmppUpstreamResponse(messageId, from, category) {

  def getPnCallbackEvent():List[PNCallbackEvent] = {
    val eventType: String = data.get("eventType").asInstanceOf[String]
    if (eventType != null) {
      val messageId = Option(data.get("messageId").asInstanceOf[String]).getOrElse("Unknown")
      val epochTime = Option(data.get("timestamp").asInstanceOf[String]).map(_.toLong).getOrElse(System.currentTimeMillis)

      List(PNCallbackEvent(messageId = messageId,
        clientId = category,
        deviceId = Option(data.get("deviceId").asInstanceOf[String]).getOrElse("Unknown"),
        eventType = eventType,
        platform = "android",
        appName = Option(data.get("appName").asInstanceOf[String]).getOrElse("NA"),
        contextId = Option(data.get("contextId").asInstanceOf[String]).getOrElse(""),
        cargo = Option(data.get("cargo").asInstanceOf[String]).getOrElse(""),
        timestamp = epochTime
      ))
    } else List()
  }
}

/** Sample from GCM
  * <message id="">
  *   <gcm xmlns="google:mobile:data">
  *     {
  *     "category":"com.example.yourapp", // to know which app sent it
  *     "data":
  *     {
  *     "hello":"world",
  *     },
  *     "message_id":"m-123",
  *     "from":"REGID"
  *     }
  *     </gcm>
  *     </message>
*/
