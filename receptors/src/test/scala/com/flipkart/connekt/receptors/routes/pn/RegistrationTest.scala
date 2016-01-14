package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{StatusCodes, MediaTypes, HttpEntity}
import com.flipkart.connekt.receptors.routes.BaseRouteTest

/**
 * @author aman.shrivastava on 10/12/15.
 */
class RegistrationTest extends BaseRouteTest {

  val registrationRoute = new Registration().register
  val appName = "RetailApp"
  val platform = "android"
  val deviceId = "b3f979dd66b8226d98007cbf6867712"
  val userId = "ACC608E23783652405FA6A7A67E70F41924"


  "Registration test" should "return Ok for save " in {
    val payload =
      s"""
        |{
        |	"deviceId": "b3f979dd66b8226d98007cbf6867712q",
        |	"state": "login",
        |	"altPush": false,
        |	"model": "SM-G530H",
        |	"token": "APA91bGUpvddvIG4rtlf_XR12M79EclmGyWIDv0Gkwj9DpEQbmei5RvWcmFxNBCF3ZBFBgRcbV_4x1jiHjxU6DkHEWMbBcafTKoARil55xnieL8n-_ymDMWmDjr8k6ZBmqk",
        |	"brand": "samsung",
        |	"appVersion": "590206",
        |	"osVersion": "4.4.4",
        |	"userId": "$userId"
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
      s"""
        |{
        |	"state": "login",
        |	"altPush": false,
        |	"model": "SM-G530H",
        |	"token": "APA91bGUpvddvIG4rtlf_XR12M79EclmGyWIDv0Gkwj9DpEQbmei5RvWcmFxNBCF3ZBFBgRcbV_4x1jiHjxU6DkHEWMbBcafTKoARil55xnieL8n-_ymDMWmDjr8k6ZBmqk",
        |	"brand": "samsung",
        |	"appVersion": "590200",
        |	"osVersion": "4.4.7",
        |	"userId": "$userId"
        |}
      """.stripMargin

    Put(s"/v1/registration/push/$platform/$appName/$deviceId", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      registrationRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }

  }

  "Registration test " should "return Ok for fetch for deviceID" in {
    Get(s"/v1/registration/push/$appName/$deviceId").addHeader(header) ~>
      registrationRoute ~>
      check {
        println("response = " + responseAs[String])
        status shouldEqual StatusCodes.OK
      }
  }

  "Registration test " should "return Ok for fetch for userID " in {
    Get(s"/v1/registration/push/$appName/users/$userId").addHeader(header) ~>
      registrationRoute ~>
      check {
        println("response = " + responseAs[String])
        status shouldEqual StatusCodes.OK
      }
  }





}
