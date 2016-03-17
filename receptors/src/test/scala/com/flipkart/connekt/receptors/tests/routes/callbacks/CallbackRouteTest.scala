package com.flipkart.connekt.receptors.tests.routes.callbacks

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.receptors.routes.callbacks.CallbackRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

/**
 * @author aman.shrivastava on 10/12/15.
 */
class CallbackRouteTest extends BaseRouteTest {

  val mssgId = "7a4df25c383d4c7a9438c478ddcadd1f2"
  val deviceId = "d7ae09474408d039ecad4534ed040f4a"
  val appName = "ConnectSampleApp"
  val headerTest = RawHeader("x-api-key", "r9qA4fF2prQ7qgX0O9NSdFl6A3q50QxB")

  "Callback Test " should "return Ok" in {
    val callbackRoute = new CallbackRoute().callback
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

    Post(s"/v1/push/callback/android/$appName/$deviceId", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      callbackRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }

    Delete(s"/v1/push/callback/$appName/$deviceId/$mssgId").addHeader(headerTest) ~> callbackRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

}
