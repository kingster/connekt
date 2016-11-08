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

import com.flipkart.concord.publisher.TPublishRequest
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.utils.DateTimeUtils

sealed case class DeviceCallbackEvent(deviceId: String, userId: String, osName: String, osVersion: String,
                                      appName: String, appVersion: String, brand: String, model: String,
                                      state: String, ts: Long, active: Boolean) extends CallbackEvent {

  override def contactId: String = throw new RuntimeException(s"`contactId` undefined for DeviceCallbackEvent")

  override def messageId: String = throw new RuntimeException(s"`messageId` undefined for DeviceCallbackEvent")

  override def eventId: String = throw new RuntimeException(s"`eventId` undefined for DeviceCallbackEvent")

  override def namespace: String = "fkint/mp/connekt/DeviceDetails"

  override def toPublishFormat: TPublishRequest = fkint.mp.connekt.DeviceDetails(
    deviceId = deviceId, userId = userId, token = null, osName = osName, osVersion = osVersion,
    appName = appName, appVersion = appVersion, brand = brand, model = model, state = state,
    ts = DateTimeUtils.getStandardFormatted(), active = active
  )
}
