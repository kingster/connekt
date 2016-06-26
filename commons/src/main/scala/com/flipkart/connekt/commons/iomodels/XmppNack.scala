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

case class XmppNack(
                        @JsonProperty("message_id")@JsonProperty(required = false) messageId: String,
                        @JsonProperty(required = false) from: String,
                        @JsonProperty(required = false) error: String,
                        @JsonProperty("error_description")@JsonProperty(required = false) errorDescription: String) extends XmppResponse {
  override def responseType(): String = "nack"
}

/**
 * Sample from GCM
 <message>
 <gcm xmlns="google:mobile:data">
 {
   "message_type":"nack",
   "message_id":"msgId1",
   "from":"bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1...",
   "error":"INVALID_JSON",
   "error_description":"InvalidJson: JSON_TYPE_ERROR : Field \"time_to_live\" must be a JSON java.lang.Number: abc"
 }
 </gcm>
</message>



 */
