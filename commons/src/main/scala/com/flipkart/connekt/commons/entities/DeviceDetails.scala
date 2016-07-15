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
package com.flipkart.connekt.commons.entities

import com.fasterxml.jackson.annotation.JsonProperty
import com.flipkart.connekt.commons.entities.bigfoot.BigfootSupport
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.utils.DateTimeUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.roundeights.hasher.Implicits._

import scala.util.Try


case class DeviceDetails(deviceId: String,
                         userId: String,
                         @JsonProperty(required = true) token: String,
                         @JsonProperty(required = false) osName: String,
                         @JsonProperty(required = true) osVersion: String,
                         @JsonProperty(required = false) appName: String,
                         @JsonProperty(required = true) appVersion: String,
                         brand: String,
                         model: String,
                         state: String = "",
                         @JsonProperty(required = false) keys: Map[String, String] = Map.empty,
                         active: Boolean = true) extends BigfootSupport[fkint.mp.connekt.DeviceDetails] {

  def toBigfootFormat: fkint.mp.connekt.DeviceDetails = {
    fkint.mp.connekt.DeviceDetails(
      deviceId = deviceId, userId = userId, token = token.sha256.hash.hex, osName = osName, osVersion = osVersion,
      appName = appName, appVersion = appVersion, brand = brand, model = model, state = state,
      ts = DateTimeUtils.getStandardFormatted(), active = active
    )
  }

  def toCallbackEvent = {
    DeviceCallbackEvent(deviceId = deviceId, userId = userId, osName = osName, osVersion = osVersion,
      appName = appName, appVersion = appVersion, brand = brand, model = model, state = state, ts = System.currentTimeMillis(), active = active)
  }

  def validate() = {
    require(Try(MobilePlatform.withName(osName)).map(!_.equals(MobilePlatform.UNKNOWN)).getOrElse(false), "a device's platform cannot be unknown")
    require(token.isDefined, "device detail's token cannot be null/empty")
    require(userId != deviceId, "`userId` cannot be equal to `deviceId`")
    require(appName.isDefined, "device detail's `appName` cannot be null/empty")
  }
}

sealed case class DeviceCallbackEvent(deviceId: String, userId: String, osName: String, osVersion: String,
                                      appName: String, appVersion: String, brand: String, model: String,
                                      state: String, ts: Long, active: Boolean) extends CallbackEvent {

  override def contactId: String = throw new RuntimeException(s"`contactId` undefined for DeviceCallbackEvent")

  override def messageId: String = throw new RuntimeException(s"`messageId` undefined for DeviceCallbackEvent")

  override def eventId: String = throw new RuntimeException(s"`eventId` undefined for DeviceCallbackEvent")
}
