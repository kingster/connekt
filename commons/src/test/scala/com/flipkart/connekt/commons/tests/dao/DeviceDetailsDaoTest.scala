package com.flipkart.connekt.commons.tests.dao

import java.util.UUID

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.tests.BaseCommonsTest

/**
 * @author aman.shrivastava on 11/12/15.
 */
class DeviceDetailsDaoTest extends BaseCommonsTest {
  val deviceId = UUID.randomUUID().toString
  val appName = "connekt"
  val deviceDetails = DeviceDetails(deviceId, UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString,
    appName, UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, true)

  "DeviceDetails Dao" should "save Device Details" in {
    noException should be thrownBy DaoFactory.getDeviceDetailsDao.saveDeviceDetails(deviceDetails)
  }

  "Device Details Dao" should "fetch Device Details" in {
    DaoFactory.getDeviceDetailsDao.fetchDeviceDetails(appName, deviceId).get shouldEqual deviceDetails
  }
}
