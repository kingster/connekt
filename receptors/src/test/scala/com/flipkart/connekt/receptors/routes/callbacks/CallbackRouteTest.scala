package com.flipkart.connekt.receptors.routes.callbacks

import akka.http.scaladsl.model.{StatusCodes, MediaTypes, HttpEntity}
import com.flipkart.connekt.receptors.routes.BaseRouteTest
import org.scalatest.Ignore

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
        status shouldEqual StatusCodes.Created
      }
  }

}
