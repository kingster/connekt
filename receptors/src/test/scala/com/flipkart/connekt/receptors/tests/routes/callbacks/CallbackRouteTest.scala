package com.flipkart.connekt.receptors.tests.routes.callbacks

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.receptors.routes.callbacks.CallbackRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

/**
 * @author aman.shrivastava on 10/12/15.
 */
class CallbackRouteTest extends BaseRouteTest {

  "Callback Test " should "return Ok" in {
    val callbackRoute = new CallbackRoute().callback
    val payload =
      """
        |{
        |	"type": "PN",
        |	"eventType": "received",
        |	"timestamp": 1449597968,
        |	"messageId": "7a4df25c383d4c7a9438c478ddcadd1f2",
        |	"contextId": "connekt-wx-01"
        |}
      """.stripMargin

    Post("/v1/push/callback/android/ConnectSampleApp/d7ae09474408d039ecad4534ed040f4a", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      callbackRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

}
