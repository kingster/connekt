package com.flipkart.connekt.receptors.tests.routes.commons

import akka.http.javadsl.server.values.FormFields
import akka.http.scaladsl.model._
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.receptors.routes.common.KeyChainRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest
import org.scalatest.Ignore

//TODO: Fix this test, multiplepart/form-data not working
@Ignore
class KeyChainRouteTest extends BaseRouteTest {

  implicit val uu = new AppUser(userId = "connekt-genesis",
    apiKey = "connekt-genesis",
    groups = "revbnt",
    contact = ""
  )
  val appName = StringUtils.generateRandomStr(6)
  val os = "windows"

  val storageRoute = new KeyChainRoute().route



  "StorageRoute PUT test" should "return Ok for successful  " in {
    val payload = "abc"
    FormFields
    val entity = FormData(("clientId", StringUtils.generateRandomStr(6)), ("secret", StringUtils.generateRandomStr(6)))

    Post(s"/v1/keychain/$appName/$os", entity).addHeader(header) ~>
      storageRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "StorageRoute GET test " should "return data" in {
    Get(s"/v1/keychain/$appName/$os").addHeader(header) ~>
      storageRoute ~>
      check {
        println("response = " + responseAs[String])
        status shouldEqual StatusCodes.OK
      }
  }


}
