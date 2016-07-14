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
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.helpers.XmppMessageIdHelper

@JsonIgnoreProperties(ignoreUnknown = true)
case class XmppReceipt(   @JsonProperty("message_id")@JsonProperty(required = true) messageId: String,
                          @JsonProperty(required = true) from: String,
                          @JsonProperty(required = true) category: String,
                          @JsonProperty(required = true) data: XmppReceiptData) extends XmppUpstreamResponse(messageId, from, category) {

  override def getPnCallbackEvent(): Option[PNCallbackEvent] = {
    val parsedMessageIdMap:Map[String,String] = XmppMessageIdHelper.parseMessageId(data.originalMessageId)
    val deviceDetails: Option[DeviceDetails] = DeviceDetailsService.getByTokenId(category, data.deviceRegistrationId).getOrElse(None)
    Some(PNCallbackEvent(messageId = parsedMessageIdMap.get(XmppMessageIdHelper.messageIdText).get,
      clientId = parsedMessageIdMap.get(XmppMessageIdHelper.clientIdText).get,
      deviceId = deviceDetails.map(_.deviceId).getOrElse(""),
      eventType = "receipt",
      platform = "android",
      appName = deviceDetails.map(_.appName).getOrElse(""),
      contextId = parsedMessageIdMap.get(XmppMessageIdHelper.contextIdText).get,
      cargo = null,
      timestamp = System.currentTimeMillis
    ))
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class XmppReceiptData (@JsonProperty("message_status") @JsonProperty(required = true) messageStatus: String,
                                   @JsonProperty("original_message_id") @JsonProperty(required = true) originalMessageId: String,
                                   @JsonProperty("device_registration_id") @JsonProperty(required = true) deviceRegistrationId: String)

/** sample data from GCM
  * <message id="">
  *   <gcm xmlns="google:mobile:data">
  *     {
  *     "category":"com.example.yourapp", // to know which app sent it
  *     "data":
  *     {
  *     “message_status":"MESSAGE_SENT_TO_DEVICE",
  *     “original_message_id”:”m-1366082849205”
  *     “device_registration_id”: “REGISTRATION_ID”
  *     },
  *     "message_id":"dr2:m-1366082849205",
  *     "message_type":"receipt",
  *     "from":"gcm.googleapis.com"
  *     }
  *     </gcm>
  *     </message>
  */
