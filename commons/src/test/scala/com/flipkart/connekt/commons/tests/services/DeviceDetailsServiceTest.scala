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
package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.tests.CommonsBaseTest

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
    device.get.get.deviceId shouldEqual deviceId
  }

  "DeviceDetails Service" should "get list of devices by userId" in {
    val deviceList = DeviceDetailsService.getByUserId(appName, accountId)
    println(deviceList)
    deviceList.get.length shouldEqual 1

    //cache version check
    val deviceList2 = DeviceDetailsService.getByUserId(appName, accountId)
    println(deviceList2)
    deviceList2.get.length shouldEqual 1
  }

  "DeviceDetails Service" should "get list of devices for given list of deviceIds" in {
    DeviceDetailsService.get(appName, List(deviceId)).get.head.deviceId shouldEqual deviceId
  }

  "DeviceDetails Service" should "get one device from cache, another from datastore" in {
    val deviceId1: String = UUID.randomUUID().toString
    val device1 = DeviceDetails(deviceId1, userId = accountId, token = tokenId, "osName", "osVersion", appName, "appVersion", "brand", "model")
    DeviceDetailsService.get(appName, List(deviceId)).get.head.deviceId shouldEqual deviceId
    noException should be thrownBy DeviceDetailsService.add(device1)
    DeviceDetailsService.get(appName, List(deviceId, deviceId1)).get.size shouldEqual 2
  }

  "DeviceDetails Service" should "update account" in {
    DeviceDetailsService.get(appName, deviceId).get.get.userId shouldEqual accountId
    val updatedAccountId: String = "ACC1-" + UUID.randomUUID().toString.take(5)
    val updatedDevice = DeviceDetails(deviceId, userId = updatedAccountId, token = tokenId, "osName",
      "osVersion", appName, "appVersion", "brand", "model")
    DeviceDetailsService.update(deviceId, updatedDevice)
    val newUpdatedDevice = DeviceDetailsService.get(appName, deviceId).get
    newUpdatedDevice.get.userId shouldEqual updatedAccountId
  }

//  "DeviceDetails Service" should "delete Account" in {
//    noException should be thrownBy DeviceDetailsService.delete(appName, deviceId)
//    DeviceDetailsService.get(appName, deviceId) shouldEqual None
//  }

}
