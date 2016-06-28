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

import com.flipkart.connekt.commons.entities.bigfoot.BigfootSupport
import com.flipkart.connekt.commons.utils.DateTimeUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import org.apache.commons.lang3.RandomStringUtils

case class PNCallbackEvent(messageId: String,
                           clientId: String,
                           deviceId: String,
                           eventType: String,
                           platform: String,
                           appName: String,
                           contextId: String,
                           cargo: String = null,
                           timestamp: Long = System.currentTimeMillis(),
                           eventId: String = RandomStringUtils.randomAlphabetic(10)) extends CallbackEvent with BigfootSupport[fkint.mp.connekt.PNCallbackEvent] {

  def toBigfootFormat: fkint.mp.connekt.PNCallbackEvent = {
    fkint.mp.connekt.PNCallbackEvent(messageId = messageId, deviceId = deviceId, platform = platform, eventType = eventType, appName = appName, contextId = contextId, cargo = cargo, timestamp = DateTimeUtils.getStandardFormatted(timestamp))
  }

  def validate() = {
    require(contextId == null || contextId.hasOnlyAllowedChars, "`contextId` field can only contain [A-Za-z0-9_\\.\\-\\:\\|] allowed chars.")
    require(eventType.isDefined, "`eventType` field cannot be empty or null.")
  }

  override def contactId: String = s"${appName.toLowerCase}$deviceId"
}
