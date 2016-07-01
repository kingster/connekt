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

import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._

object XmppMessageIdHelper {
  def generateMessageId(message: ConnektRequest, device:DeviceDetails):String = message.id + ":" + device.deviceId + ":" + message.contextId.orEmpty

  def parseMessageIdTo(messageStr:String):(String,String) = {
    val splitString = messageStr.split(':')
    val originalMessageId = if (StringUtils.isNullOrEmpty(messageStr)) "" else splitString(0)
    val context = if ( splitString.length >= 3) splitString(2) else ""
    (originalMessageId,context)
  }

}
