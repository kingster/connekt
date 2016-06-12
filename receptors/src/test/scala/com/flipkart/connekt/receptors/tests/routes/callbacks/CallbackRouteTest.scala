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
package com.flipkart.connekt.receptors.tests.routes.callbacks

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.receptors.routes.callbacks.CallbackRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

class CallbackRouteTest extends BaseRouteTest {

  val mssgId = "7a4df25c383d4c7a9438c478ddcadd1f2"
  val deviceId = "d7ae09474408d039ecad4534ed040f4a"
  val appName = "RetailApp"
  val mobilePlatform = "android"

  "Callback Test " should "return Ok" in {
    val callbackRoute = new CallbackRoute().route
    val payload =
      s"""
         |{
         |	"type": "PN",
         |	"eventType": "received",
         |	"timestamp": 1449597968,
         |	"messageId": "$mssgId",
         |	"contextId": "connekt-wx-01"
         |}
      """.stripMargin

    Post(s"/v1/push/callback/$mobilePlatform/$appName/$deviceId", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      callbackRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }

    Delete(s"/v1/push/callback/$mobilePlatform/$appName/$deviceId/$mssgId").addHeader(header) ~> callbackRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

}
