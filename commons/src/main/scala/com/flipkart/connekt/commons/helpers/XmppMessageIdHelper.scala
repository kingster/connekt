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
package com.flipkart.connekt.commons.helpers

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNRequestInfo}
import com.flipkart.connekt.commons.utils.CompressionUtils
import com.flipkart.connekt.commons.utils.StringUtils._

sealed case class XmppMessageId(@JsonProperty("dId") deviceId: String, @JsonProperty("mId") messageId: String, @JsonProperty("cId") clientId: String, @JsonInclude(Include.NON_NULL) @JsonProperty("ctx") contextId: Option[String], @JsonProperty("aN") appName: String)

object XmppMessageIdHelper {

  def generateMessageId(message: ConnektRequest, deviceId: String): String = {
    val xmppMessageId = XmppMessageId(
      deviceId = deviceId,
      messageId = message.id,
      clientId = message.clientId,
      contextId = message.contextId,
      appName = message.channelInfo.asInstanceOf[PNRequestInfo].appName
    )
    CompressionUtils.deflate(xmppMessageId.getJson).get
  }

  def parseMessageId(messageStr: String): Option[XmppMessageId] = {
    CompressionUtils.inflate(messageStr).toOption.map(_.getObj[XmppMessageId])
  }

}
