package com.flipkart.connekt.commons.entities.fabric

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
sealed trait ChannelFabric

trait EmailFabric extends ChannelFabric {
  def getSubject(id: String, context: ObjectNode): String
  def getBodyHtml(id: String, context: ObjectNode): String
}

trait PNFabric {
  def getData(id: String, context: ObjectNode): String
}