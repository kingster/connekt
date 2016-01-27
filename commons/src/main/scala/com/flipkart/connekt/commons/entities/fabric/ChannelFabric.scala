package com.flipkart.connekt.commons.entities.fabric

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, EmailRequestData, PNRequestData}

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
sealed trait ChannelFabric {
}

trait EmailFabric extends ChannelFabric {
  def getSubject(id: String, context: ObjectNode): String

  def getBodyHtml(id: String, context: ObjectNode): String

  def renderData(id: String, context: ObjectNode): ChannelRequestData =
    EmailRequestData(getSubject(id, context), getBodyHtml(id, context))
}

trait PNFabric extends ChannelFabric {
  def getData(id: String, context: ObjectNode): ObjectNode

  def renderData(id: String, context: ObjectNode): ChannelRequestData =
    PNRequestData(getData(id, context))
}