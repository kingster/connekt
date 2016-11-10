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
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{GCMDispatcherPrepare, SMTPDispatcher}
import com.flipkart.connekt.busybees.streams.flows.formaters.AndroidChannelFormatter
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.GCMResponseHandler
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

class SMTPEmailTopologyTest extends TopologyUTSpec {

  "AndroidTopology Test" should "run" in {

    val credentials = KeyChainManager.getSimpleCredential("Flipkart-SMTP").get

    lazy implicit val poolClientFlow = Http().cachedHostConnectionPoolHttps[GCMRequestTracker]("fcm.googleapis.com", 443)

    val cRequest = s"""
                     |{ "id" : "123456789",
                     |	"channel": "EMAIL",
                     |	"sla": "H",
                     |	"channelData": {
                     |		"type": "EMAIL",
                     |		"data": {
                     |			"message": "Hello Kinshuk. GoodLuck!",
                     |			"title": "Kinshuk GCM Push Test",
                     |			"id": "${System.currentTimeMillis()}",
                     |			"triggerSound": true,
                     |			"notificationType": "Text"
                     |		}
                     |	},
                     |	"channelInfo" : {
                     |	    "type" : "EMAIL",
                     |	    "ackRequired": true,
                     |    	"delayWhileIdle": true,
                     |     "platform" :  "android",
                     |     "appName" : "RetailApp",
                     |     "deviceIds" : ["81adb899c58c9c8275e2b1ffa2d03861"]
                     |	},
                     |  "clientId" : "123456",
                     |	"meta": {}
                     |}
                   """.stripMargin.getObj[ConnektRequest]


    val result = Source.single(cRequest)
      .via(new RenderFlow().flow)
      .via(new AndroidChannelFormatter(64)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
      .via(new GCMDispatcherPrepare().flow)
      .via(new SMTPDispatcher().flow)
      .via(new GCMResponseHandler().flow)
      .runWith(Sink.head)

    val response = Await.result(result, 80.seconds)

    println(response)

    assert(response != null)
  }

}
