package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.services.BigfootService
import fkint.mp.comm_pf.DeviceDetails
import org.joda.time.format.DateTimeFormat

/**
 * Created by nidhi.mehla on 03/02/16.
 */
class BigFootServiceTest extends CommonsBaseTest {

  val deviceId = UUID.randomUUID().toString
  val userId = "ACC-" + UUID.randomUUID().toString
  val token = "TOKEN-" + UUID.randomUUID().toString

  "BigFoot Service " should " return success " in {
    val deviceDetails = DeviceDetails(deviceId, userId, token, "osName", "osVersion",
      "appName", "appVersion", "brand", "model", "state",
      DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss").print(System.currentTimeMillis()))
    BigfootService.ingest(deviceDetails).get shouldEqual true
  }

}
