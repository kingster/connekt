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
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils._

case class ConnektRequest(@JsonProperty(required = false) id: String,
                          contextId: Option[String],
                          channel: String,
                          @JsonProperty(required = true) sla: String,
                          templateId: Option[String],
                          scheduleTs: Option[Long],
                          expiryTs: Option[Long],
                          @JsonProperty(required = true) channelInfo: ChannelRequestInfo,
                          @JsonProperty(required = false) channelData: ChannelRequestData,
                          @JsonProperty(required = false) channelDataModel: ObjectNode = getObjectNode,
                          meta: Map[String, String]) {

  def this(id: String, contextId: Option[String], channel: String, sla: String, templateId: Option[String],
           scheduleTs: Option[Long], expiryTs: Option[Long], channelInfo: ChannelRequestInfo,
           channelData: ChannelRequestData, channelDataModel: ObjectNode) {
    this(id, contextId, channel, sla, templateId, scheduleTs, expiryTs, channelInfo, channelData, channelDataModel, Map.empty[String,String])
  }

  def validate() = {
    require(templateId.map(StencilService.get(_).isDefined).getOrElse(Option(channelData).isDefined), "given template doesn't exist")
    require(contextId.map(_.hasOnlyAllowedChars).getOrElse(true), "`contextId` field can only contain [A-Za-z0-9_.-:|] allowed chars.")
    require(sla.isDefined, "`sla` field can cannot be null or empty.")
    require(meta != null, "`meta` field cannot be null. It is optional but non-null")
  }
}
