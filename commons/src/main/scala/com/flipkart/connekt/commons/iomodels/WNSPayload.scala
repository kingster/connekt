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

import akka.http.scaladsl.model.{ContentType, ContentTypes}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

import scala.xml.{Node, XML}

case class WNSPayloadEnvelope(messageId: String, token: String, appName: String, deviceId: String, time_to_live: Long, contextId: String, wnsPayload: WNSPayload, meta: Map[String, Any])

object WindowsNotificationType extends Enumeration {
  val toast, tile, badge, raw = Value
}

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(Array(
  new Type(value = classOf[WNSToastPayload], name = "toast"),
  new Type(value = classOf[WNSTilePayload], name = "tile"),
  new Type(value = classOf[WNSBadgePayload], name = "badge"),
  new Type(value = classOf[WNSRawPayload], name = "raw")
))
abstract class WNSPayload {
  def getType: String

  def getContentType: ContentType

  def getBody: Array[Byte]
}

abstract class WNSXMLPayload(xml: Node) extends WNSPayload {

  def getBody = xml.toString().getBytes

  def getContentType = ContentTypes.`text/xml(UTF-8)`
}

abstract class WNSBinaryPayload(octetStream: String) extends WNSPayload {

  def getBody = octetStream.getBytes

  def getContentType = ContentTypes.`application/octet-stream`
}

case class WNSToastPayload(body: String) extends WNSXMLPayload(XML.loadString(body)) {

  override def getType: String = "wns/toast"
}

case class WNSTilePayload(body: String) extends WNSXMLPayload(XML.loadString(body)) {

  override def getType: String = "wns/tile"
}

case class WNSBadgePayload(body: String) extends WNSXMLPayload(XML.loadString(body)) {

  override def getType: String = "wns/badge"
}

case class WNSRawPayload(body: String) extends WNSBinaryPayload(body) {

  override def getType: String = "wns/raw"
}
