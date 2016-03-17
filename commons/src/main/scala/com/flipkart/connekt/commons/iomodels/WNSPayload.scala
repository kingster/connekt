/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import akka.http.scaladsl.model.{ContentType, ContentTypes}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.node.ObjectNode

import scala.xml.Node

/**
 * @author aman.shrivastava on 08/02/16.
 */

case class WNSPayloadEnvelope(messageId: String, token: String, appName: String, deviceId: String, wnsPNType: WNSPayload)

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
  def getWnsType: String

  def getContentType: ContentType

  def getPayload: Array[Byte]
}

abstract class WNSXMLPayload extends WNSPayload {
  def toXML: Node

  def getPayload = toXML.toString.getBytes

  def getContentType = ContentTypes.`text/xml(UTF-8)`

}

abstract class WNSBinaryPayload extends WNSPayload {
  def toOctetStream: String

  def getPayload = toOctetStream.toString.getBytes

  def getContentType = ContentTypes.`application/octet-stream`
}

case class WNSToastPayload(title: String, message: String, actions: ObjectNode) extends WNSXMLPayload {

  def toXML: Node =
    <toast launch={actions.toString}>
      <visual>
        <binding template="ToastText02">
          <text id="1">{title}</text>
          <text id="2">{message}</text>
        </binding>
      </visual>
    </toast>

  override def getWnsType: String = "wns/toast"

}

case class WNSTilePayload(title: String, message: String, actions: ObjectNode) extends WNSXMLPayload {
  def toXML: Node = ???

  override def getWnsType: String = "wns/tile"

}

case class WNSBadgePayload(title: String, message: String, actions: ObjectNode) extends WNSXMLPayload {
  def toXML: Node = ???

  override def getWnsType: String = "wns/badge"

}

case class WNSRawPayload(title: String, message: String, actions: ObjectNode) extends WNSBinaryPayload {
  override def toOctetStream: String = ???

  override def getWnsType: String = "wns/raw"
}
