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
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.services.DeviceDetailsService

case class XmppReceipt(   @JsonProperty("message_id")@JsonProperty(required = true) messageId: String,
                          @JsonProperty(required = true) from: String,
                          @JsonProperty(required = true) category: String,
                          @JsonProperty(required = true) data: XmppReceiptData) extends XmppUpstreamResponse(messageId, from, category){

  def getPnCallbackEvent():List[PNCallbackEvent] = {
    val (originalMsgId, context) = com.flipkart.connekt.commons.helpers.XmppMessageIdHelper.parseMessageIdTo(data.originalMessageId)
    val deviceDetails:DeviceDetails = DeviceDetailsService.getByTokenId(category, data.deviceRegistrationId).getOrElse(None).getOrElse(null)
    List(PNCallbackEvent(messageId = originalMsgId,
      clientId = from,
      deviceId = if (deviceDetails == null ) "" else deviceDetails.deviceId,
      eventType = "receipt",
      platform = "android",
      appName = category,
      contextId = context,
      cargo = "",
      timestamp = System.currentTimeMillis
    ))
  }
}

case class XmppReceiptData(@JsonProperty("message_status") messageStatus: String,
                                   @JsonProperty("original_message_id") originalMessageId: String,
                                   @JsonProperty("device_registration_id") deviceRegistrationId: String)


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
