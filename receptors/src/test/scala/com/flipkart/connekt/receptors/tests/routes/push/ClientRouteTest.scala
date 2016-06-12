/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.receptors.tests.routes.push

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

class ClientRouteTest extends BaseRouteTest {

  val clientName = "clientId"

  val clientPayload =
    s"""
       |{
       |  "userId":"$clientName",
       |  "contact":"connekt-dev@flipkart.com"
       |}
     """.stripMargin

  val testHeader = RawHeader("x-api-key", "f3YVn32EC3GSyn423NeDbk6zGV2E67Qx2Ee5ZGXyZSS6dRWU")

  "Create Client Test" should "create client" in {
    Post(s"/v1/client/create/", HttpEntity(MediaTypes.`application/json`, clientPayload)).addHeader(testHeader) ~>
      clientRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Get client Test " should "get client info" in {
    Get(s"/v1/client/$clientName").addHeader(testHeader) ~>
      clientRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Delete client Test" should "delete " in {
    DaoFactory.getUserInfoDao.removeUserById(clientName)
  }
}
