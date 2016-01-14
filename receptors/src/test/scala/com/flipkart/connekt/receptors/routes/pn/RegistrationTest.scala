package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{StatusCodes, MediaTypes, HttpEntity}
import com.flipkart.connekt.receptors.routes.BaseRouteTest

/**
 * @author aman.shrivastava on 10/12/15.
 */
class RegistrationTest extends BaseRouteTest {

  val registrationRoute = new Registration().register
  val appName = "RetailApp"
  val platform = "ANDROID"
  val deviceId = "b3f979dd66b8226d98007cbf6867712q"


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
        |	"userId": "ACC608E23783652405FA6A7A67E70F419248"
        |}
      """.stripMargin

    Post(s"/v1/registration/push/$platform/$appName", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      registrationRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }

  }


  "Registration test" should "return Ok for update " in {
    val payload =
      """
        |{
        |	"state": "login",
        |	"altPush": false,
        |	"model": "SM-G530H",
        |	"token": "APA91bGUpvddvIG4rtlf_XR12M79EclmGyWIDv0Gkwj9DpEQbmei5RvWcmFxNBCF3ZBFBgRcbV_4x1jiHjxU6DkHEWMbBcafTKoARil55xnieL8n-_ymDMWmDjr8k6ZBmqk",
        |	"brand": "samsung",
        |	"appVersion": "590200",
        |	"osVersion": "4.4.7",
        |	"userId": "ACC608E23783652405FA6A7A67E70F419248"
        |}
      """.stripMargin

    Put(s"/v1/registration/push/$platform/$appName/$deviceId", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      registrationRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }

  }


  "Registration test " should "return Ok for fetch " in {
    Get(s"/v1/registration/push/$appName/$deviceId").addHeader(header) ~>
      registrationRoute ~>
      check {
        println("response = " + responseAs[String])
        status shouldEqual StatusCodes.OK
      }
  }



}
