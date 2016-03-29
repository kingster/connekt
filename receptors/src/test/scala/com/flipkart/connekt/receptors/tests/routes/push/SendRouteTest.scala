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

import java.util.UUID

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

class SendRouteTest extends BaseRouteTest {

  val appName = "retailqa"
  val platform = "android"
  val unknownPlatfrom = "unknown"
  val deviceId = "bbd505411b210e38b15142bd6a0de0f6"
  private val accountId = "ACC-" + UUID.randomUUID().toString.take(5)
  private val tokenId = "Token-" + UUID.randomUUID().toString.take(5)

  var device = DeviceDetails(deviceId, userId = accountId, token = tokenId, appName, "osVersion", appName, "appVersion", "brand", "model")

  "Send PN Test" should "return created response status" in {

    val payload =
      s"""
         |{
         |	"channel": "push",
         |	"sla": "H",
         |	"scheduleTs": 12312312321,
         |	"expiryTs": 3243243224,
         |	"channelInfo": {
         |		"type": "PN",
         |		"ackRequired": true,
         |   "deviceId" : ["$deviceId", "nfkjadnfkl"],
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

    Post(s"/v1/send/push/$unknownPlatfrom/$appName", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      unicastRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }
  }

  "Send PN Test -- to Account" should "return created response status" in {

    val payload =
      s"""
         |{
         |	"channel": "push",
         |	"sla": "H",
         |	"scheduleTs": 12312312321,
         |	"expiryTs": 3243243224,
         |	"channelInfo": {
         |		"type": "PN",
         |		"ackRequired": true,
         |   "deviceId" : [],
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

    Post(s"/v1/send/push/$unknownPlatfrom/$appName/user/$accountId", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      unicastRoute ~>
      check {
        status shouldEqual StatusCodes.NotFound
      }

    DeviceDetailsService.add(device)
    
    Post(s"/v1/send/push/$platform/$appName/user/$accountId", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      unicastRoute ~>
      check {
        status shouldEqual StatusCodes.Created
      }
  }


}
