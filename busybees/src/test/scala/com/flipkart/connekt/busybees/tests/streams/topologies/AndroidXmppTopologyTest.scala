package com.flipkart.connekt.busybees.tests.streams.topologies

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

import akka.stream.FlowShape
import akka.stream.scaladsl.{Merge, GraphDSL, Sink, Source}
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{GcmXmppDispatcher, GCMXmppDispatcherPrepare}
import com.flipkart.connekt.busybees.streams.flows.formaters.{AndroidXmppChannelFormatter, AndroidHttpChannelFormatter, AndroidChannelFormatter}
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.{XmppUpstreamHandler, XmppDownstreamHandler, GCMResponseHandler}
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.iomodels.{PNCallbackEvent, ConnektRequest}
import com.flipkart.connekt.commons.services.{ConnektConfig, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration._

class AndroidXmppTopologyTest extends TopologyUTSpec {

  "AndroidXmppTopology Test" should "run" in {

    val credentials = KeyChainManager.getGoogleCredential("ConnektSampleApp").get

    val cRequest = s"""
                      |{
                      |	"channel": "PN",
                      |	"sla": "H",
                      |	"stencilId": "retail-app-base-0x23",
                      |	"scheduleTs": 12312312321,
                      |	"channelData": {
                      |		"type": "PN",
                      |		"data": {
                      |			"message": "Hello Kinshuk. GoodLuck!",
                      |			"title": "Kinshuk GCM Push Test",
                      |			"id": "${System.currentTimeMillis()}",
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
                      |     "appName" : "RetailApp",
                      |     "deviceIds" : ["fdb4d2071b8f53ad8f877774f0c38d07"]
                      |	},
                      |	"meta": {}
                      |}
                   """.stripMargin.getObj[ConnektRequest]

    val ioDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")

    lazy val xmppOnlyTopology = GraphDSL.create() {
      implicit b ⇒
        val fmtAndroid = b.add(new AndroidXmppChannelFormatter(64)(ioDispatcher).flow)
        val gcmXmppPrepare = b.add(new GCMXmppDispatcherPrepare().flow)
        val gcmXmppPoolFlow = b.add(new GcmXmppDispatcher)
        val downstreamHandler = b.add(new XmppDownstreamHandler()(mat, ioDispatcher).flow)
        val upstreamHandler = b.add(new XmppUpstreamHandler()(mat, ioDispatcher).flow)
        val merger = b.add(Merge[PNCallbackEvent](2))

        fmtAndroid ~> gcmXmppPrepare ~> gcmXmppPoolFlow.in
                                            gcmXmppPoolFlow.out0 ~> downstreamHandler ~> merger.in(0)
                                            gcmXmppPoolFlow.out1 ~> upstreamHandler ~> merger.in(1)
        FlowShape(fmtAndroid.in, merger.out)
    }

    val result = Source.single(cRequest)
                  .via(xmppOnlyTopology)
                  .runWith(Sink.head)

    val response = Await.result(result, 80.seconds)

    println(response)

    assert(response != null)
  }

}
