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
  val clientApiKey = "clientApiKey"

  val clientPayload =
    s"""
       |{
       |  "userId":"$clientName",
       |  "contact":"connekt-dev@flipkart.com",
       |  "groups":"admin",
       |  "apiKey": "$clientApiKey"
       |}
     """.stripMargin

  val sendPayload =
    s"""
       |{
       |	"channel": "push",
       |	"sla": "H",
       |	"scheduleTs": 12312312321,
       |	"expiryTs": 3243243224,
       |	"channelInfo": {
       |		"type": "PN",
       |		"ackRequired": true,
       |   "deviceIds" : ["bbd505411b210e38b15142bd6a0de0f6", "nfkjadnfkl"],
       |		"delayWhileIdle": true
       |	},
       |	"channelData": {
       |		"type": "PN",
       |		"data": {
       |			"message": "_phantomastray_",
       |			"title": "Do not go gentle into that good night.",
       |			"id": "pqwx2p2x321122228w2t1wxt",
       |			"triggerSound": true,
       |			"notificationType": "Text"
       |		}
       |	},
       |	"meta": {}
       |}        """.stripMargin

  val permissionPayload =
    s"""
       |{
       |  "resources": "SEND_CONNEKTSAMPLEAPP"
       |}
     """.stripMargin


  val testHeader = RawHeader("x-api-key", "f3YVn32EC3GSyn423NeDbk6zGV2E67Qx2Ee5ZGXyZSS6dRWU")
  val sendHeader = RawHeader("x-api-key", clientApiKey)

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

  "Grant client Permission " should " grant permissions" in {
    Post(s"/v1/client/grant/USER/$clientName", HttpEntity(MediaTypes.`application/json`, permissionPayload)).addHeader(testHeader) ~>
      clientRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }
  }

  "Send PN Test" should "have access " in {
    Post(s"/v1/send/push/unknown/CONNEKTSAMPLEAPP", HttpEntity(MediaTypes.`application/json`, sendPayload)).addHeader(sendHeader) ~>
    sendRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }
  }

  "Revoke client Permission " should " revoke permissions" in {
    Post(s"/v1/client/revoke/USER/$clientName", HttpEntity(MediaTypes.`application/json`, permissionPayload)).addHeader(testHeader) ~>
      clientRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }
  }

  "Send PN Test" should "not have access " in {
    Post(s"/v1/send/push/unknown/CONNEKTSAMPLEAPP", HttpEntity(MediaTypes.`application/json`, sendPayload)).addHeader(sendHeader) ~>
      sendRoute ~>
      check {
        status shouldEqual StatusCodes.Unauthorized
      }
  }

  "Delete client Test" should "delete " in {
    DaoFactory.getUserInfoDao.removeUserById(clientName)
  }
}
