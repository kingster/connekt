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
package com.flipkart.connekt.commons.entities.fabric

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, EmailRequestData, PNRequestData}
import com.flipkart.connekt.commons.utils.StringUtils._

sealed trait ChannelFabric {
}

trait EmailFabric extends ChannelFabric {
  def getSubject(id: String, context: ObjectNode): String

  def getBodyHtml(id: String, context: ObjectNode): String

  def renderData(id: String, context: ObjectNode): ChannelRequestData =
    EmailRequestData(getSubject(id, context), getBodyHtml(id, context))
}

trait PNFabric extends ChannelFabric {
  def getData(id: String, context: ObjectNode): String

  def renderData(id: String, context: ObjectNode): ChannelRequestData =
    PNRequestData(getData(id, context).getObj[ObjectNode])
}

trait PNPlatformFabric extends PNFabric {
  def getTopic(id: String, context: ObjectNode): String
}
