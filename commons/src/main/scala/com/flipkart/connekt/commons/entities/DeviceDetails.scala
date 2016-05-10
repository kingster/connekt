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
                         active: Boolean = true) extends BigfootSupport[fkint.mp.connekt.DeviceDetails] {

  def toBigfootFormat: fkint.mp.connekt.DeviceDetails = {
    fkint.mp.connekt.DeviceDetails(
      deviceId = deviceId, userId = userId, token = token.sha256.hash.hex, osName = osName, osVersion = osVersion,
      appName = appName, appVersion = appVersion, brand = brand, model = model, state = state,
      ts = DateTimeUtils.getStandardFormatted(), active = active
    )
  }

  def validate() = {
    require(Try(MobilePlatform.withName(osName)).map(!_.equals(MobilePlatform.UNKNOWN)).getOrElse(false), "a device's platform cannot be unknown")
    require(token.isDefined, "device detail's token cannot be null/empty")
    require(appName.isDefined, "device detail's app name cannot be null/empty")
  }
}
