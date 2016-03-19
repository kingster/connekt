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
