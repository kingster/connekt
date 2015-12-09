package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{StatusCodes, MediaTypes, HttpEntity}
import com.flipkart.connekt.receptors.routes.BaseRouteTest

/**
 * @author aman.shrivastava on 10/12/15.
 */
class RegistrationTest extends BaseRouteTest {

  val registrationRoute = new Registration().register

  "Registration test" should "return Ok for save " in {
    val payload =
      """
        |{
        |	"deviceId": "b3f979dd66b8226d98007cbf6867712q",
        |	"state": "login",
        |	"altPush": false,
        |	"model": "SM-G530H",
        |	"token": "APA91bGUpvddvIG4rtlf_XR12M79EclmGyWIDv0Gkwj9DpEQbmei5RvWcmFxNBCF3ZBFBgRcbV_4x1jiHjxU6DkHEWMbBcafTKoARil55xnieL8n-_ymDMWmDjr8k6ZBmqk",
        |	"brand": "samsung",
        |	"appVersion": "590206",
        |	"osVersion": "4.4.4",
        |	"osName": "ANDROID",
        |	"userId": "ACC608E23783652405FA6A7A67E70F419248",
        |	"appName": "RetailApp"
        |}
      """.stripMargin

    Post("/v1/push/device/registration/save", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      registrationRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }

  }

  "Registration test " should "return Ok for fetch " in {
    Get("/v1/push/device/registration/fetch/RetailApp/b3f979dd66b8226d98007cbf6867712q").addHeader(header) ~>
      registrationRoute ~>
      check {
        println("response = " + responseAs[String])
        status shouldEqual StatusCodes.OK
      }
  }

}
