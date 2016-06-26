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

case class XmppReceipt(
                          @JsonProperty("message_id")@JsonProperty(required = false) messageId: String,
                          @JsonProperty(required = false) from: String,
                          @JsonProperty(required = false) category: String,
                          data: XmppReceiptData) extends XmppResponse {
  override def responseType(): String = "receipt"
}

case class XmppReceiptData(@JsonProperty("message_status") messageStatus: String,
                                   @JsonProperty("original_message_id") originalMessageId: String,
                                   @JsonProperty("device_registration_id") deviceRegistrationId: String)


/**
  sample data from GCM
  <message id="">
  <gcm xmlns="google:mobile:data">
  {
      "category":"com.example.yourapp", // to know which app sent it
      "data":
      {
         “message_status":"MESSAGE_SENT_TO_DEVICE",
         “original_message_id”:”m-1366082849205”
         “device_registration_id”: “REGISTRATION_ID”
      },
      "message_id":"dr2:m-1366082849205",
      "message_type":"receipt",
      "from":"gcm.googleapis.com"
  }
  </gcm>
</message>
  */
