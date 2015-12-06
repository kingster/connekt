package com.flipkart.connekt.receptors.service

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 11/24/15
 */
object RegistrationService {
  val deviceDetailsDao = DaoFactory.getDeviceDetailsDao

  def saveDeviceDetails(deviceDetails: DeviceDetails) = Try {
    deviceDetailsDao.saveDeviceDetails(deviceDetails)
    ConnektLogger(LogFile.SERVICE).debug(s"Save DeviceDetails: ${deviceDetails.toString}")
    true
  }

  def getDeviceDetails(appName: String, deviceId: String) = Try {
    deviceDetailsDao.fetchDeviceDetails(appName, deviceId)
  }
}
