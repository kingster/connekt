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
import com.flipkart.connekt.busybees.streams.flows.dispatchers.APNSDispatcher
import com.flipkart.connekt.busybees.streams.flows.formaters.IOSChannelFormatter
import com.flipkart.connekt.busybees.streams.sources.RateControl
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

    val deviceId = StringUtils.generateRandomStr(32)

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
                     |      "deviceId" : ["$deviceId"]
                     |	},
                     |	"meta": {}
                     |}
                   """.stripMargin.getObj[ConnektRequest]


    val result = Source.single(cRequest)
      .via(new RenderFlow)
      .via(new IOSChannelFormatter(16)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
      .via(new APNSDispatcher())
      .runWith(Sink.head)

    val response = Await.result(result, 60.seconds)

    println(response)

    Thread.sleep(2000)

    assert(null != response)


  }

}
