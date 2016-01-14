package com.flipkart.connekt.commons.tests.dao

import java.util.UUID

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.tests.BaseCommonsTest
import com.flipkart.connekt.commons.utils.{StringUtils, UtilsEnv}

/**
 * @author aman.shrivastava on 11/12/15.
 */
class DeviceDetailsDaoTest extends BaseCommonsTest {

  val deviceId = UUID.randomUUID().toString
  val appName = "connekt"
  val userId = "ACCIDZA86P"
  val tokenId = "40XQocR2LOWxmNOMSQ3i&2GwvZc&79zehJ%&3&M%O4Bs#9R3RLzNiB!9TIOBkLDB#rB83q!ntFdqw@OPuqc$A@Y0^xAy&V@57eFajqAcJ2k1ktVa0^KxQzbdG"

  val deviceDetails = DeviceDetails(deviceId, userId, tokenId, UUID.randomUUID().toString, UUID.randomUUID().toString,
    appName, UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, true)

  "DeviceDetails Dao" should "save Device Details" in {
    noException should be thrownBy DaoFactory.getDeviceDetailsDao.add(deviceDetails)
  }

  "Device Details Dao" should "fetch Device Details" in {
    DaoFactory.getDeviceDetailsDao.get(appName, deviceId).get shouldEqual deviceDetails
  }

  "Device Details Dao" should " get by userId" in{
    DaoFactory.getDeviceDetailsDao.getByUserId(appName,userId).nonEmpty shouldBe true
    DaoFactory.getDeviceDetailsDao.getByUserId(appName,StringUtils.generateRandomStr(12)).nonEmpty shouldBe false
    println(DaoFactory.getDeviceDetailsDao.getByUserId(appName,userId).head )
    DaoFactory.getDeviceDetailsDao.getByUserId(appName,userId).head shouldEqual deviceDetails

  }

  "Device Details Dao" should " get by token" in{
    DaoFactory.getDeviceDetailsDao.getByTokenId(appName,tokenId).nonEmpty shouldBe true
    DaoFactory.getDeviceDetailsDao.getByTokenId(appName,StringUtils.generateRandomStr(12)).nonEmpty shouldBe false

    println(DaoFactory.getDeviceDetailsDao.getByTokenId(appName,tokenId).get)
    DaoFactory.getDeviceDetailsDao.getByTokenId(appName,tokenId).get shouldEqual deviceDetails

  }
}
