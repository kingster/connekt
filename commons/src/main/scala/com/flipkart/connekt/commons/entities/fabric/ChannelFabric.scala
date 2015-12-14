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
  def getSubject(context: ObjectNode): String
  def getBodyHtml(context: ObjectNode): String
}

trait PNFabric {
  def getData(context: ObjectNode): String
}