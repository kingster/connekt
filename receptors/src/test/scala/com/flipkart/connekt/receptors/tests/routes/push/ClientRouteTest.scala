package com.flipkart.connekt.receptors.tests.routes.push

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, MediaTypes, HttpEntity}
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

/**
 * Created by nidhi.mehla on 22/03/16.
 */
class ClientRouteTest extends BaseRouteTest {

  val clientName = "clientId"

  val clientPayload =
    s"""
       |{
       |  "userId":"$clientName",
       |  "contact":"contact"
       |}
     """.stripMargin

  val testHeader = RawHeader("x-api-key", "f3YVn32EC3GSyn423NeDbk6zGV2E67Qx2Ee5ZGXyZSS6dRWU")

  "Create Client Test" should "create client" in {
    Post(s"v1/client/", HttpEntity(MediaTypes.`application/json`, clientPayload)).addHeader(testHeader) ~>
      clientRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }
  }

  "Create Client Test" should "create client" in {
    Post(s"v1/client/", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(testHeader) ~>
      clientRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }
  }


}
