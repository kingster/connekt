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

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.{Sink, Source}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpDispatcher
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.{ConnektConfig, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.scalatest.Ignore

import scala.concurrent.Await
import scala.concurrent.duration._

@Ignore
class RepeatSource2GCMBenchmarkTopologyTest extends TopologyUTSpec with Instrumented {

  override def beforeAll() = {
    super.beforeAll()
    HttpDispatcher.init(ConnektConfig.getConfig("react").get)
  }

  val counter: AtomicLong = new AtomicLong(0)
  "RepeatSource2GCMBenchmarkTopologyTest" should "log gcm dispatch rates for a vanilla graph" in {
    val source = Source.repeat({
      val appName = "ConnektSampleApp"
      val deviceId = Seq[String]("b25f2cdce678c67707228818e64fb4a0")
      val messageId = UUID.randomUUID().toString
      val gcmPayload =
        s"""
          |{
          |	"registration_ids": ["dJeq9vJfXaM:APA91bGz7OM8-7r8lhTFfOm8gYAgBnsuzKMYXfpeVFs1-pjLoswR_02GGi3Wjh-Ai_RJ07Dah3G83F7qbVSD4-2BTxkw7VsFRUGCijPgm0SDDi1aIllGySJuMWcjeAC_ODtiNXi0POk1"],
          |	"delay_while_idle": false,
          | "dry_run" : true,
          |	"data": {
          |		"message": "Hello theGhost",
          |		"id": "7a4df25c383d4c7a9438c478ddcadd1f",
          |		"triggerSound ": true,
          |		"notificationType": "Text",
          |		"title": "[Direct] Do not go gentle into that good night",
          |		"messageId": "$messageId"
          |	}
          |}
        """.stripMargin

      val requestEntity = HttpEntity(ContentTypes.`application/json`, gcmPayload)
      val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(appName).get.apiKey))
      val httpRequest = HttpRequest(HttpMethods.POST, "/gcm/send", requestHeaders, requestEntity)
      val requestTrace = GCMRequestTracker(messageId, deviceId, appName, "", "test", Map())
      (httpRequest, requestTrace)
    })

    val qps = meter("android.send")


    val requestExecutor = HttpDispatcher.gcmPoolClientFlow.map(rT => {
      qps.mark()
      rT._1.foreach(_.entity.getString.getObj[ObjectNode])
      if(0 == (counter.incrementAndGet() % 1000))
        println(s"RepeatAndroidBenchmarkTopology #Rate: MR[${qps.getMeanRate}}], 1MR[${qps.getOneMinuteRate}}] upto ${counter.get()} messages")
    })

    //Run the benchmark topology
    val rF = source.via(requestExecutor).runWith(Sink.ignore)

    Await.result(rF, 120.seconds)
  }

}
