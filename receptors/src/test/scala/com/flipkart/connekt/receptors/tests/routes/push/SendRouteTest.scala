package com.flipkart.connekt.receptors.tests.routes.push

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */

class SendRouteTest extends BaseRouteTest {

  val appName = "ConnectSampleApp"
  val platform = "android"
  val deviceId = "bbd505411b210e38b15142bd6a0de0f6"
  "Unicast Test" should "return success response" in {
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
          |   "deviceId" : ["$deviceId"],
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

      Post(s"/v1/send/push/unicast/$platform/$appName", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
        unicastRoute ~>
        check {
          status shouldEqual StatusCodes.OK
      }
  }

}
