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
import com.flipkart.connekt.commons.entities.bigfoot.BigfootSupport
import com.flipkart.connekt.commons.utils.DateTimeUtils

case class PNCallbackEvent(@JsonProperty(required = false) messageId: String,
                           @JsonProperty(required = false) deviceId: String,
                           eventType: String,
                           @JsonProperty(required = false) platform: String,
                           @JsonProperty(required = false) appName: String,
                           contextId: String,
                           @JsonProperty(required = false) cargo: String = null,
                           timestamp: Long = System.currentTimeMillis()) extends CallbackEvent with BigfootSupport[fkint.mp.connekt.PNCallbackEvent] {

  def toBigfootFormat : fkint.mp.connekt.PNCallbackEvent = {
    fkint.mp.connekt.PNCallbackEvent(messageId = messageId, deviceId = deviceId, platform = platform, eventType = eventType, appName = appName, contextId = contextId, cargo = cargo, timestamp = DateTimeUtils.getStandardFormatted(timestamp))
  }
  
}
