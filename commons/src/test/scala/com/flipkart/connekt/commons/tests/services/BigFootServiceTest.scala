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

import com.flipkart.connekt.commons.services.BigfootService
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import fkint.mp.connekt.DeviceDetails
import org.joda.time.format.DateTimeFormat
import org.scalatest.Ignore

@Ignore
class BigFootServiceTest extends CommonsBaseTest {

  val deviceId = "UT-" + UUID.randomUUID().toString
  val userId = "ACC-" + UUID.randomUUID().toString
  val token = "TOKEN-" + UUID.randomUUID().toString

  "BigFoot Service " should " return success " in {
    val deviceDetails = DeviceDetails(deviceId, userId, token, "osName", "osVersion",
      "appName", "appVersion", "brand", "model", "state",
      DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss").print(System.currentTimeMillis()))
    BigfootService.ingest(deviceDetails).get shouldEqual true
  }

}
