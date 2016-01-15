package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{StatusCodes, HttpEntity, MediaTypes}
import com.flipkart.connekt.receptors.routes.BaseRouteTest

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
class UnicastTest extends BaseRouteTest {

  val unicastRoute = new Unicast().unicast
  "Unicast Test" should "return success response" in {
      val payload =
        """
          |{
          |	"channel": "PN",
          |	"sla": "H",
          |	"templateId": "retail-app-base-0x23",
          |	"scheduleTs": 12312312321,
          |	"expiryTs": 3243243224,
          |	"channelInfo": {
          |		"type": "PN",
          |		"ackRequired": true,
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

      Post("/v1/send/push/unicast/android/ConnectSampleApp/bbd505411b210e38b15142bd6a0de0f6", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
        unicastRoute ~>
        check {
          status shouldEqual StatusCodes.Created
      }


  }

}
