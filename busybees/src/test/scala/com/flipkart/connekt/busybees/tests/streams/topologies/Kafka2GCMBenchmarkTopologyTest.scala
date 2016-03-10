package com.flipkart.connekt.busybees.tests.streams.topologies

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.Attributes
import akka.stream.scaladsl.{Sink, Source}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpDispatcher
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.services.{ConnektConfig, DeviceDetailsService, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.scalatest.Ignore

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

/**
 *
 *
 * @author durga.s
 * @version 3/10/16
 */
@Ignore
class Kafka2GCMBenchmarkTopologyTest extends TopologyUTSpec {

  override def beforeAll() = {
    super.beforeAll()
    HttpDispatcher.init(ConnektConfig.getConfig("busybees.akka.http").get)
  }

  val counter: AtomicLong = new AtomicLong(0)

  "Kafka2GCMBenchmarkTopologyTest" should "log gcm dispatch rates for a vanilla graph" in {

    val topic = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.head
    val kSource = new KafkaSource[ConnektRequest](getKafkaConsumerHelper, topic, 1)(Promise[String]().future)

    val requestExecutor = HttpDispatcher.gcmPoolClientFlow.map(rT => {
      rT._1.foreach(_.entity.getString.getObj[ObjectNode])
      if(0 == (counter.incrementAndGet() % 1000))
        ConnektLogger(LogFile.SERVICE).info(s"Processed ${counter.get()} messages by ${System.currentTimeMillis()}")
    })

    //Run the benchmark topology
    val rF = Source.fromGraph(kSource).map(transform2GCMRequest).withAttributes(Attributes.asyncBoundary).via(requestExecutor).runWith(Sink.ignore)

    Await.result(rF, 120.seconds)
  }

  private def transform2GCMRequest(request: ConnektRequest): (HttpRequest, GCMRequestTracker) = {
    val messageId = UUID.randomUUID().toString
    val pNRequestInfo = request.channelInfo.asInstanceOf[PNRequestInfo]
    val deviceId = List[String](pNRequestInfo.deviceId.head)
    val gcmPayload =
      s"""
          |{
          |	"registration_ids": ["${DeviceDetailsService.get(pNRequestInfo.appName, pNRequestInfo.deviceId.head).get.get.token}"],
          |	"delay_while_idle": false,
          | "dry_run" : true,
          |	"data": ${request.channelData.asInstanceOf[PNRequestData].data.toString}
          |}
        """.stripMargin

    val requestEntity = HttpEntity(ContentTypes.`application/json`, gcmPayload)
    val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(pNRequestInfo.appName).get.apiKey))
    val httpRequest = new HttpRequest(HttpMethods.POST, "/gcm/send", requestHeaders, requestEntity)
    val requestTrace = GCMRequestTracker(messageId, deviceId, pNRequestInfo.appName)
    (httpRequest, requestTrace)
  }
}
