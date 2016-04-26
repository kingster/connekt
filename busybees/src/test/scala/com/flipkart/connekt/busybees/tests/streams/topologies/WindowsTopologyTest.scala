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
import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source, _}
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.WNSDispatcherPrepare
import com.flipkart.connekt.busybees.streams.flows.formaters.WindowsChannelFormatter
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.WNSResponseHandler
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNCallbackEvent, WNSPayloadEnvelope}
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._

class WindowsTopologyTest extends TopologyUTSpec {
  "WindowsTopology Test" should "run" in {

    val deviceId = "TEST-WIN-102"

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId,
        userId = "",
        token = "https://hk2.notify.windows.com/?token=AwYAAACgHkoOVVWGZKeRPzsjQFZQZkIlSVgGQmMJYml%2b4maOyhnwvG%2bKTrLGVkkLnRJ0SKzVHWOaQ9KKlUPFWfsG5yMi7rACONMp7w6Pko1x8H2nqZJlqzNhfQylhnXomv8DPxw%3d",
        osName = "windows", osVersion = "8.0", appName = "retailapp", appVersion = "UT", brand = "UT", model = "UT"
      )
    )

    val cRequest = s"""
                      |{
                      |	"channel": "PN",
                      |	"sla": "H",
                      |	"templateId": "retail-app-base-0x23",
                      |	"channelData": {
                      |		"type": "PN",
                      |		"data": {
                      |      "type" : "toast",
                      |      "message" : "This is a test PN",
                      |      "title" : "Hello Aman",
                      |      "actions" : {
                      |		      "url": "http://www.flipkart.com/offers/electronics?notificationId=test-IMFDN60SOD&omnitureData=test-IMFDN60SOD_ME",
                      |		      "fallback": null,
                      |		      "params": {
                      |			       "tabKey": "categoryPage4",
                      |			       "screenName": "foz",
                      |			       "preferredWidgetKey": "dealWidget1:categoryPage4",
                      |			       "pageKey": "tab"
                      |		      },
                      |		      "screenType": "multiWidgetPage",
                      |		      "omnitureData": null,
                      |		      "tracking": {
                      |			        "omnitureData": "test-IMFDN60SOD_ME",
                      |			        "notificationId": "test-IMFDN60SOD"
                      |		      },
                      |		      "loginType": "LOGIN_NOT_REQUIRED",
                      |		      "type": "NAVIGATION"
                      |	     }
                      |		}
                      |	},
                      |	"channelInfo" : {
                      |	    "type" : "PN",
                      |	    "ackRequired": true,
                      |    	"delayWhileIdle": true,
                      |      "platform" :  "windows",
                      |      "appName" : "retailapp",
                      |      "deviceIds" : ["$deviceId"]
                      |	},
                      |	"meta": {}
                      |}
                   """.stripMargin.getObj[ConnektRequest]

    lazy implicit val poolClientFlow = Http().superPool[WNSRequestTracker]()

    lazy val graph = GraphDSL.create() {
      implicit b ⇒

        val out = Sink.foreach[PNCallbackEvent](println)

        val render = b.add(new RenderFlow().flow)
        val formatter = b.add(new WindowsChannelFormatter(1)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
        val dispatcher = b.add(new WNSDispatcherPrepare().flow)

        val pipeInletMerge = b.add(MergePreferred[WNSPayloadEnvelope](1))

        val pipe = b.add(poolClientFlow)
        val responseHandler = b.add(new WNSResponseHandler())

        val retryMapper = b.add(Flow[WNSRequestTracker].map(t => {
          ConnektLogger(LogFile.PROCESSORS).error("retryMapper" + t)
          t.request
        }))

        Source(List(cRequest, cRequest)) ~> render ~> formatter ~>  pipeInletMerge

        pipeInletMerge.out ~> dispatcher  ~> pipe ~> responseHandler.in

        responseHandler.out1 ~> retryMapper ~> pipeInletMerge.preferred
        responseHandler.out0 ~> out

        ClosedShape
    }

    RunnableGraph.fromGraph(graph).run()

    Thread.sleep(15000)

    assert(null != true)
  }

}
