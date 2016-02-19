package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
case class ConnektRequest(@JsonProperty(required = false) id: String,
                          channel: String,
                          sla: String,
                          templateId: Option[String],
                          scheduleTs: Option[Long],
                          expiryTs: Option[Long],
                          channelInfo: ChannelRequestInfo,
                          @JsonProperty(required = false) channelData: ChannelRequestData,
                          @JsonProperty(required = false) channelDataModel: ObjectNode = StringUtils.getObjectNode,
                          meta: Map[String, String]) {
  def validate() : Boolean = {
    templateId.map(StencilService.get(_).isDefined).getOrElse(Option(channelData).isDefined)
  }
}
