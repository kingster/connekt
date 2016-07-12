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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "message_type",
  defaultImpl = classOf[XmppUpstreamData]
)
@JsonSubTypes(Array(
  new Type(value = classOf[XmppReceipt], name = "receipt")
))
abstract class XmppUpstreamResponse (
                              @JsonProperty("message_id")@JsonProperty(required = true) messageId: String,
                              @JsonProperty(required = true) from: String,
                              @JsonProperty(required = true) category: String) {
  def getPnCallbackEvent():List[PNCallbackEvent]
}

/**
import com.flipkart.connekt.commons.utils.StringUtils._

object Sample {
  def main (args: Array[String]) {
    val drJsonString = "{\"data\":{\"message_status\":\"MESSAGE_SENT_TO_DEVICE\",\"device_registration_id\":\"dbp9UlEJTbg:APA91bHZxuX6BnPLW4J_NZSLh7TeXN00eOGZY99npmVBVpB8MVciZDHlhOL4F7RRCktfcYQf90_7HgsWvLLh8C4aavinu6Mp0nt3RKCvy0EaopnRDqK_XwFwTeOkb8SAeQUCBwbmGPXA\",\"message_sent_timestamp\":\"1468231318886\",\"original_message_id\":\"1468231313418:subirDeviceForXmppTest:\"},\"time_to_live\":0,\"from\":\"gcm.googleapis.com\",\"message_id\":\"dr2:1468231313418:subirDeviceForXmppTest:\",\"message_type\":\"receipt\",\"category\":\"com.flipkart.android.connekt.example\"}"
    val upJsonString = "{\"data\":{\"appName\":\"ConnektSampleApp\",\"eventType\":\"RECEIVED\",\"type\":\"PN\",\"cargo\":\"{\\\"numFKNotificationInTray\\\":\\\"0\\\",\\\"totalDiskSpace\\\":\\\"13851541504\\\",\\\"emptyDiskSpace\\\":\\\"3044192256\\\",\\\"networkType\\\":\\\"WiFi\\\",\\\"serviceProvider\\\":\\\"Vodafone IN\\\"}\",\"deviceId\":\"dbp9UlEJTbg:APA91bHZxuX6BnPLW4J_NZSLh7TeXN00eOGZY99npmVBVpB8MVciZDHlhOL4F7RRCktfcYQf90_7HgsWvLLh8C4aavinu6Mp0nt3RKCvy0EaopnRDqK_XwFwTeOkb8SAeQUCBwbmGPXA\",\"timestamp\":\"1468231318619\"},\"time_to_live\":0,\"from\":\"dbp9UlEJTbg:APA91bHZxuX6BnPLW4J_NZSLh7TeXN00eOGZY99npmVBVpB8MVciZDHlhOL4F7RRCktfcYQf90_7HgsWvLLh8C4aavinu6Mp0nt3RKCvy0EaopnRDqK_XwFwTeOkb8SAeQUCBwbmGPXA\",\"message_id\":\"1468231313418:subirDeviceForXmppTest:\",\"category\":\"com.flipkart.android.connekt.example\"}"

    val upstreamMsg: Try[XmppUpstreamResponse] = Try(upJsonString.getObj[XmppUpstreamResponse]) recover {
      case ex: Exception => {q
        ConnektLogger(LogFile.CLIENTS).debug("Serialising to upstreamdata", ex)
        upJsonString.getObj[XmppUpstreamData]
      }
    }
    ConnektLogger(LogFile.CLIENTS).debug(upstreamMsg)
  }
}
**/
