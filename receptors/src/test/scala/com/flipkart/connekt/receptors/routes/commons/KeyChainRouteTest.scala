package com.flipkart.connekt.receptors.routes.commons

import java.util.Date

import akka.http.scaladsl.model._
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.receptors.routes.BaseRouteTest
import com.flipkart.connekt.receptors.routes.common.KeyChainRoute

/**
 * Created by nidhi.mehla on 18/02/16.
 */
class KeyChainRouteTest extends BaseRouteTest {

  implicit val uu = new AppUser(userId = "connekt-genesis",
    apiKey = "connekt-genesis",
    groups = "revbnt",
    contact = "")

  val storageRoute = new KeyChainRoute().route



  "StorageRoute PUT test" should "return Ok for successful  " in {
    val payload = "abc"
    val entity: RequestEntity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, payload.getBytes)
    Put(s"/v1/storage/123", entity).addHeader(header) ~>
      storageRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "StorageRoute GET test " should "return data" in {
    Get(s"/v1/storage/123").addHeader(header) ~>
      storageRoute ~>
      check {
        println("response = " + responseAs[String])
        status shouldEqual StatusCodes.OK
      }
  }


}
