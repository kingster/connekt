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
package com.flipkart.connekt.busybees.tests.streams.topologies

import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Sink, Source}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GCMDispatcherPrepare
import com.flipkart.connekt.busybees.streams.flows.formaters.AndroidChannelFormatter
import com.flipkart.connekt.busybees.streams.sources.RateControl
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

class AndroidTopologyTest extends TopologyUTSpec {

  "AndroidTopology Test" should "run" in {

    val credentials = KeyChainManager.getGoogleCredential("ConnektSampleApp").get

    val httpDispatcher = new GCMDispatcherPrepare


    lazy implicit val poolClientFlow = Http().cachedHostConnectionPoolHttps[GCMRequestTracker]("android.googleapis.com", 443)

    val cRequest = """
                     |{
                     |	"channel": "PN",
                     |	"sla": "H",
                     |	"templateId": "retail-app-base-0x23",
                     |	"scheduleTs": 12312312321,
                     |	"expiryTs": 3243243224,
                     |	"channelData": {
                     |		"type": "PN",
                     |		"data": {
                     |			"message": "Hello Kinshuk. GoodLuck!",
                     |			"title": "Kinshuk GCM Push Test",
                     |			"id": "123456789",
                     |			"triggerSound": true,
                     |			"notificationType": "Text"
                     |
                     |		}
                     |	},
                     |	"channelInfo" : {
                     |	    "type" : "PN",
                     |	    "ackRequired": true,
                     |    	"delayWhileIdle": true,
                     |     "platform" :  "android",
                     |     "appName" : "ConnectSampleApp",
                     |     "deviceId" : ["513803e45cf1b344ef494a04c9fb650a"]
                     |	},
                     |	"meta": {}
                     |}
                   """.stripMargin.getObj[ConnektRequest]


    val result = Source.single(cRequest)
      .via(new RateControl[ConnektRequest](2, 1, 2))
      .via(new RenderFlow)
      .via(new AndroidChannelFormatter(64)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
      .via(httpDispatcher)
      .via(poolClientFlow)
      .runWith(Sink.head)

    val response = Await.result(result, 60.seconds)

    response._1.isSuccess shouldEqual true

    val httpResponse = Await.result(response._1.get.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)

    println(httpResponse)

    assert(httpResponse != null)


  }

}
