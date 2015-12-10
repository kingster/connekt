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
          |    "channel": "PN",
          |    "sla": "H",
          |    "templateId": "retail-app-base-0x23",
          |    "scheduleTs": 12312312321,
          |    "expiryTs": 3243243224,
          |    "channelData": {
          |        "type": "PN",
          |        "data": "{        \"message\": \"This is a test PN\",        \"title\": \"Test PN\",        \"id\": \"pqwx2p2x321122228w2t1wxt\",        \"triggerSound\" : true,        \"notificationType\" : \"Text\"}",
          |        "ackRequired": true,
          |        "delayWhileIdle": true
          |    },
          |    "meta": {}
          |}
        """.stripMargin

      Post("/v1/push/unicast/android/ConnectSampleApp/bbd505411b210e38b15142bd6a0de0f6", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
        unicastRoute ~>
        check {
          status shouldEqual StatusCodes.Created
      }


  }

}
