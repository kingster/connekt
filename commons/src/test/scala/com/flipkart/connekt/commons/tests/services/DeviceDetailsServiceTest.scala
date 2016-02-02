package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import org.scalatest.Ignore

/**
 * Created by nidhi.mehla on 28/01/16.
 */
@Ignore
class DeviceDetailsServiceTest extends CommonsBaseTest {

  private val deviceId: String = UUID.randomUUID().toString
  private val appName = "App"
  private val accountId = "ACC-" + UUID.randomUUID().toString.take(5)
  private val tokenId = "Token-" + UUID.randomUUID().toString.take(5)

  var device = DeviceDetails(deviceId, userId = accountId, token = tokenId, "osName", "osVersion", appName, "appVersion", "brand", "model")

  "DeviceDetails Service" should "add device" in {
    noException should be thrownBy DeviceDetailsService.add(device)
  }

  "DeviceDetails Service" should "get device by deviceId" in {
    val device = DeviceDetailsService.get(appName, deviceId)
    device.get.deviceId shouldEqual deviceId
  }

  "DeviceDetails Service" should "get list of devices by userId" in {
    val deviceList = DeviceDetailsService.getByUserId(appName, accountId)
    deviceList.length shouldEqual 1
  }

  "DeviceDetails Service" should "get list of devices by token Id" in {
    val device = DeviceDetailsService.getByTokenId(appName, tokenId).get
    device.deviceId shouldEqual deviceId
  }

  "DeviceDetails Service" should "update account" in {
    DeviceDetailsService.get(appName, deviceId).get.userId shouldEqual accountId
    val updatedAccountId: String = "ACC1-" + UUID.randomUUID().toString.take(5)
    val updatedDevice = DeviceDetails(deviceId, userId = updatedAccountId, token = tokenId, "osName",
      "osVersion", appName, "appVersion", "brand", "model")
    DeviceDetailsService.update(deviceId, updatedDevice)
    val newUpdatedDevice = DeviceDetailsService.get(appName, deviceId).get
    newUpdatedDevice.userId shouldEqual updatedAccountId
  }


}
