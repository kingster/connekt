package com.flipkart.connekt.busybees.streams.topologies

import akka.stream.scaladsl.{Sink, Source}
import com.flipkart.connekt.busybees.streams.TopologyUTSpec
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.APNSDispatcher
import com.flipkart.connekt.busybees.streams.flows.formaters.IOSChannelFormatter
import com.flipkart.connekt.busybees.streams.sources.RateControl
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.{KeyChainManager, DeviceDetailsService}
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by kinshuk.bairagi on 05/02/16.
 */
class iOSTopology extends TopologyUTSpec {

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
                     |	"scheduleTs": 12312312321,
                     |	"expiryTs": 3243243224,
                     |	"channelData": {
                     |		"type": "PN",
                     |		"data": {
                     |      "alert" : "Message received from Kinshuk ${deviceId.substring(0,4)}",
                     |      "sound" : "default",
                     |      "badge" : 0
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
      .via(new RateControl[ConnektRequest](2, 1, 2))
      .via(new RenderFlow)
      .via(new IOSChannelFormatter)
      .via(new APNSDispatcher())
      .runWith(Sink.head)

    val response = Await.result(result, 60.seconds)

    println(response)

    Thread.sleep(2000)

    assert(null != response)


  }

}
