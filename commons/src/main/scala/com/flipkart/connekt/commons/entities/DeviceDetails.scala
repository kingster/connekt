package com.flipkart.connekt.commons.entities

import com.flipkart.connekt.commons.utils.DateTimeUtils

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
case class DeviceDetails(deviceId: String, userId: String, token: String, osName: String, osVersion: String,
                         appName: String, appVersion: String, brand: String, model: String, state: String = "",
                         active: Boolean = true) {

  def toBigfootEntity: fkint.mp.connekt.DeviceDetails = {
    fkint.mp.connekt.DeviceDetails(
      deviceId = deviceId, userId = userId, token = token, osName = osName, osVersion = osVersion,
      appName = appName, appVersion = appVersion, brand = brand, model = model, state = state,
      ts = DateTimeUtils.getStandardFormatted(), active = active
    )
  }

}

