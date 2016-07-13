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
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.util.Try

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "message_type",
  defaultImpl = classOf[XmppUpstreamData]
)
@JsonSubTypes(Array(
  new Type(value = classOf[XmppAck], name = "ack"),
  new Type(value = classOf[XmppNack], name = "nack"),
  new Type(value = classOf[XmppReceipt], name = "receipt"),
  new Type(value = classOf[XmppControl], name = "control")
))
abstract class XmppResponse () {
}

abstract class XmppUpstreamResponse (
                                      @JsonProperty("message_id")@JsonProperty(required = true) messageId: String,
                                      @JsonProperty(required = true) from: String,
                                      @JsonProperty(required = true) category: String) extends XmppResponse {
  def getPnCallbackEvent():Option[PNCallbackEvent]
}

abstract class XmppDownstreamResponse extends XmppResponse {
  def responseType(): String
}
