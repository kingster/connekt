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
import akka.stream.scaladsl._
import com.flipkart.connekt.busybees.models.OpenWebRequestTracker
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.OpenWebDispatcherPrepare
import com.flipkart.connekt.busybees.streams.flows.formaters.OpenWebChannelFormatter
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.OpenWebResponseHandler
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

class OpenWebTopologyTest extends TopologyUTSpec {

  "OpenWeb TopologyTest" should "run" in {

    val deviceId1 = "Chrome-test-50+"
    val deviceId2 = "Firefox-test"
    val deviceId3 = "Chrome-test-50-"
    val appName = "fklite"

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId1,
        userId = "",
        token = "https://android.googleapis.com/gcm/send/eHW_Dj74uEM:APA91bEoKPRfR4VWK8bs5p5LqERfL9iG9RAZeqQvzSoE1wlF3N0varb5v6x8ryenmYXFyD-HJQ6M4t7V8lb3oaRqnV12X-1nSA5K8Yh0jw2dnHRHWyKvdfCZVR8puK_YZ7WNQe9ZwmQL",
        osName = "openweb", osVersion = "6.0.1", appName, appVersion = "UT", brand = "UT", model = "",
        keys = Map("auth" -> "lYykEnete-tm2E1h0Izaog==", "p256dh" -> "BJCpis23C7P7HHV7GRS3y6wqq7VaaPT5uTscQG64mES6U4pegJ80b4zYxUu3TEpgrAXjIYLpLlgfriqq0XgWE6A=")
      )
    )

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId2,
        userId = "",
        token = "https://updates.push.services.mozilla.com/push/v1/gAAAAABXOvMWgkCNeTlt45ka7aax6zP929q_pIdBLQG7FMhPfTOdxSGNcEALzcai1NszuF1hZoRYBbxWSMC0lFNs6Z2b2PNQVp_YZriAUcZJUZHluwrBPTtYwu41fXwzpbxSfPR8KKUv",
        osName = "openweb", osVersion = "6.0.1", appName, appVersion = "UT", brand = "UT", model = "",
        keys = Map("auth" -> "51VXF3Q7hUepYjvQTxfJEw", "p256dh" -> "BOZneqTrDtUmom4n9cVsuKaBOsc0U7Ueim5gfS0KfF9xg8KOqUrUCTtVIyioC0h54mpBfYQxglt3nfjbx92OCyE")
      )
    )

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId3,
        userId = "",
        token = "https://android.googleapis.com/gcm/send/eHW_Dj74uEM:APA91bEoKPRfR4VWK8bs5p5LqERfL9iG9RAZeqQvzSoE1wlF3N0varb5v6x8ryenmYXFyD-HJQ6M4t7V8lb3oaRqnV12X-1nSA5K8Yh0jw2dnHRHWyKvdfCZVR8puK_YZ7WNQe9ZwmQL",
        osName = "openweb", osVersion = "6.0.1", appName, appVersion = "UT", brand = "UT", model = "",
        keys = Map.empty
      )
    )

    val cRequest = s"""
                      |{
                      |	"channel": "PN",
                      |	"sla": "H",
                      |	"channelData": {
                      |		"type": "PN",
                      |		"data": {
                      |			"title": "Hello Nidhi",
                      |			"message": "Nidhi test for OpenWeb notification chrome."
                      |		}
                      |	},
                      |	"channelInfo": {
                      | "platform" : "openweb",
                      |		"appName": "fklite",
                      |		"type": "PN",
                      |		"ackRequired": true,
                      |		"delayWhileIdle": true,
                      |		"deviceIds": ["$deviceId3" , "$deviceId1" , "$deviceId2"]
                      |	},
                      |	"meta": {
                      |		"client": "Openweb"
                      |	}
                      |}
                      |""".stripMargin.getObj[ConnektRequest]

    val openWebPoolClientFlow = Http().superPool[OpenWebRequestTracker]()

    val result = Source.repeat(cRequest)
      .via(new RenderFlow().flow)
      .via(new OpenWebChannelFormatter(64)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
      .via(new OpenWebDispatcherPrepare().flow)
      .via(openWebPoolClientFlow)
      .via(new OpenWebResponseHandler().flow)
      .runWith(Sink.head)

    val response = Await.result(result, 80.seconds)

    println(response)

    assert(response != null)

    Thread.sleep(15000)
  }
}
