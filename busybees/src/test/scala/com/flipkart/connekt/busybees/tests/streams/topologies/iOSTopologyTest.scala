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

/**
 * Created by kinshuk.bairagi on 05/02/16.
 */
class iOSTopologyTest extends TopologyUTSpec {

  "iOSTopology Test" should "run" in {

    val deviceId = StringUtils.generateRandomStr(32)

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId,
        userId = "",
        token = "e27fff706bef4e958ebd4ae798fb6517fdfdd9ccc3e899caf8cfcfee31716da4",
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
      .via(new IOSChannelFormatter(16)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
      .via(new APNSDispatcher())
      .runWith(Sink.head)

    val response = Await.result(result, 60.seconds)

    println(response)

    Thread.sleep(2000)

    assert(null != response)


  }

}
