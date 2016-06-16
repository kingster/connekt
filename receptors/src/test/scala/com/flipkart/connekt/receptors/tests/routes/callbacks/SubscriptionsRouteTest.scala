package com.flipkart.connekt.receptors.tests.routes.callbacks

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.receptors.routes.callbacks.{CallbackRoute, SubscriptionsRoute}
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

/**
  * Created by harshit.sinha on 08/06/16.
  */
class SubscriptionsRouteTest extends BaseRouteTest {

  val userId = "connekt-insomnia"
  val headerTest = RawHeader("x-api-key", "e    ")

  "subscriptionsRoute Test " should "return Ok" in {
    val subscriptionRoute = new SubscriptionsRoute().route
    val payload =
      s"""
         |{
         |	"event": "PN",
         |	"eventType": "received",
         |	"timestamp": 1449597968,
         |	"contextId": "connekt-wx-01"
         |}
      """.stripMargin

    Post(s"/v1/subscription", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      subscriptionRoute ~>
      check {
        println("client received response: " + response.toString())
        status shouldEqual StatusCodes.OK
      }

//    Delete(s"/v1/push/callback/$appName/$deviceId/$mssgId").addHeader(headerTest) ~> callbackRoute ~>
//      check {
//        status shouldEqual StatusCodes.OK
//      }
  }
}
