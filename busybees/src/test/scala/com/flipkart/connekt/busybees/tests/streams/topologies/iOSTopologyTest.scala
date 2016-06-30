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

import akka.stream.scaladsl.{Sink, Source}
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{APNSDispatcherPrepare, APNSDispatcher}
import com.flipkart.connekt.busybees.streams.flows.formaters.IOSChannelFormatter
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.APNSResponseHandler
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

class iOSTopologyTest extends TopologyUTSpec {

  "iOSTopology Test" should "run" in {

    val deviceId = "TEST-123-IOS"

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId,
        userId = "",
        token = "6b1e059bb2a51d03d37384d1493aaffbba4edc58f8e21eb2f80ad4851875ee25",
        osName = "ios", osVersion = "UT", appName = "RetailApp", appVersion = "UT", brand = "UT", model = "UT"
      )
    )

    val cRequest = s"""
                     |{
                     |	"channel": "PN",
                     |	"sla": "H",
                     |	"channelData": {
                     |		"type": "PN",
                     |		"data": {
                     |      "title" : "Hello" ,
                     |      "message" : "Message ${deviceId.substring(0,4)} from Kinshuk "
                     |		}
                     |	},
                     |	"channelInfo" : {
                     |	    "type" : "PN",
                     |	    "ackRequired": true,
                     |    	"delayWhileIdle": true,
                     |      "platform" :  "ios",
                     |      "appName" : "RetailApp",
                     |      "deviceIds" : ["$deviceId"]
                     |	},
                     |	"meta": {},
                     |  "clientId" : "random",
                     |  "id" : "12345678980"
                     |}
                   """.stripMargin.getObj[ConnektRequest]

    lazy val ioDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")

    val result = Source.tick(0.seconds, 1000.milliseconds, cRequest)
      .via(new RenderFlow().flow)
      .via(new IOSChannelFormatter(16)(ioDispatcher).flow)
      .via(new APNSDispatcherPrepare().flow)
      .via(new APNSDispatcher(32)(ioDispatcher).flow)
      .via(new APNSResponseHandler().flow)
      .runWith(Sink.foreach(println))

    val response = Await.result(result, 20.seconds)

    println(response)

    assert(null != response)


  }

}
