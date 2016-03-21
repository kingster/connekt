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

case class XmppReceipt(@JsonProperty("message_type") messageType: String,
                          @JsonProperty("message_id")@JsonProperty(required = false) messageId: String,
                          @JsonProperty(required = false) from: String,
                          @JsonProperty(required = false) category: String,
                          data: XmppReceiptData) extends XmppResponse {
  override def responseType(): String = messageType
}

case class XmppReceiptData(@JsonProperty("message_status") messageStatus: String,
                                   @JsonProperty("original_message_id") originalMessageId: String,
                                   @JsonProperty("device_registration_id") deviceRegistrationId: String)
