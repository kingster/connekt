package com.flipkart.connekt.busybees.tests.streams.topologies

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.{Sink, Source}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpDispatcher
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.services.{ConnektConfig, KeyChainManager}
import org.scalatest.Ignore

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}
import com.flipkart.connekt.commons.utils.StringUtils._
/**
 *
 *
 * @author durga.s
 * @version 3/10/16
 */
@Ignore
class GCMBenchmarkTopologyTest extends TopologyUTSpec {

  override def beforeAll() = {
    super.beforeAll()
    HttpDispatcher.init(ConnektConfig.getConfig("busybees.akka.http").get)
  }

  val counter: AtomicLong = new AtomicLong(0)
  "GCMBenchmarkTopologyTest" should "log gcm dispatch rates for a vanilla graph" in {
    val source = Source.tick(FiniteDuration(0, TimeUnit.SECONDS), FiniteDuration(1, TimeUnit.MILLISECONDS), {
      val appName = "ConnektSampleApp"
      val deviceId = List[String]("b25f2cdce678c67707228818e64fb4a0")
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

      println(s"JSON: $gcmPayload")

      val requestEntity = HttpEntity(ContentTypes.`application/json`, gcmPayload)
      val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(appName).get.apiKey))
      val httpRequest = new HttpRequest(HttpMethods.POST, "/gcm/send", requestHeaders, requestEntity)
      val requestTrace = GCMRequestTracker(messageId, deviceId, appName)
      (httpRequest, requestTrace)
    })

    val requestExecutor = HttpDispatcher.gcmPoolClientFlow.map(rT => {
      rT._1.foreach(_.entity.getString)
      if(0 == (counter.incrementAndGet() % 1000))
        println(s"Processed ${counter.get()} messages by ${System.currentTimeMillis()}")
    })

    val rF = source.via(requestExecutor).runWith(Sink.ignore)

    Await.result(rF, 120.seconds)
  }
}
