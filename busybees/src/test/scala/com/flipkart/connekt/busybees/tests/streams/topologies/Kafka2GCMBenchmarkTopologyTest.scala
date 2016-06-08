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
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.{ConnektConfig, DeviceDetailsService, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.scalatest.Ignore

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

@Ignore
class Kafka2GCMBenchmarkTopologyTest extends TopologyUTSpec with Instrumented {

  override def beforeAll() = {
    super.beforeAll()
    HttpDispatcher.init(ConnektConfig.getConfig("react").get)
  }

  val counter: AtomicLong = new AtomicLong(0)
  val counterTS: AtomicLong = new AtomicLong(System.currentTimeMillis)

  "Kafka2GCMBenchmarkTopologyTest" should "log gcm dispatch rates for a vanilla graph" in {

    val topic = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.head
    val kSource = new KafkaSource[ConnektRequest](getKafkaConsumerHelper, topic)(Promise[String]().future)
    val qps = meter("android.send")

    val repeatSource = Source.repeat {
      """
        |{
        |	"channel": "PN",
        |	"sla": "H",
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
        |     "deviceIds" : ["513803e45cf1b344ef494a04c9fb650a"]
        |	},
        |	"meta": {
        |   "x-perf-test" : "true"
        | }
        |}
      """.stripMargin.getObj[ConnektRequest]
    }

    val requestExecutor = HttpDispatcher.gcmPoolClientFlow.map(rT => {
      rT._1.foreach(_.entity.getString.getObj[ObjectNode])
      qps.mark()
      if (0 == (counter.incrementAndGet() % 1000)) {
        val now = System.currentTimeMillis
        val rate = 1000 * 1000 / (now - counterTS.getAndSet(now))
        ConnektLogger(LogFile.SERVICE).info(s"Processed ${counter.get()} messages by $now, RATE = $rate,  MR[${qps.getMeanRate}}], 1MR[${qps.getOneMinuteRate}}]")
      }
    })

    //Run the benchmark topology
    val rF = Source.fromGraph(kSource).mapAsync(64)(transform2GCMRequest).async.via(requestExecutor).runWith(Sink.ignore)

    Await.result(rF, 180.seconds)
  }

  val futureDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")

  private def transform2GCMRequest(request: ConnektRequest) = Future {
    val messageId = UUID.randomUUID().toString
    val pNRequestInfo = request.channelInfo.asInstanceOf[PNRequestInfo]
    val deviceId = Seq[String](pNRequestInfo.deviceIds.head)
    val gcmPayload =
      s"""
         |{
         |	"registration_ids": ["${DeviceDetailsService.get(pNRequestInfo.appName, pNRequestInfo.deviceIds.head).get.get.token}"],
         |	"delay_while_idle": false,
         | "dry_run" : true,
         |	"data": ${request.channelData.asInstanceOf[PNRequestData].data.toString}
         |}
        """.stripMargin

    val requestEntity = HttpEntity(ContentTypes.`application/json`, gcmPayload)
    val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(pNRequestInfo.appName).get.apiKey))
    val httpRequest = HttpRequest(HttpMethods.POST, "/gcm/send", requestHeaders, requestEntity)
    val requestTrace = GCMRequestTracker(messageId, "", deviceId, pNRequestInfo.appName, "test", Map())
    //    println("Rinning in thread " + Thread.currentThread().getName)
    (httpRequest, requestTrace)
  }(futureDispatcher)
}
