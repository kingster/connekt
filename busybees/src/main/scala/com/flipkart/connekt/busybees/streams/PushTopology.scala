package com.flipkart.connekt.busybees.streams

import java.net.URL

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{APNSDispatcher, HttpPrepare, RequestIdentifier, WNSDispatcher}
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters.{AndroidChannelFormatter, IOSChannelFormatter, WindowsChannelFormatter}
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.{GCMResponseHandler, WNSResponseHandler}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
case class wnsResponse(appName: String, requestId: String)

class PushTopology(consumer: KafkaConsumerHelper) extends ConnektTopology[PNCallbackEvent] {

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = ActorMaterializer()

  override def source: Source[ConnektRequest, NotUsed] = Source.fromGraph(GraphDSL.create(){ implicit b =>
    val topics = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.filter(_.equalsIgnoreCase("push_86726ba26f92fbbc71480880b8fbaef0076ea5f39fad95a008a7d0211ca441d0"))
    ConnektLogger(LogFile.PROCESSORS).info(s"Creating composite source for topics: ${topics.toString()}")

    val merge = b.add(Merge[ConnektRequest](topics.size))
    for(portNum <- 0 to merge.n - 1) {
      new KafkaSource[ConnektRequest](consumer, topic = topics(portNum)) ~> merge.in(portNum)
    }

    SourceShape(merge.out)
  })

  override def transform = Flow.fromGraph(GraphDSL.create(){ implicit  b =>
    val gcmPoolClientFlow = Http().cachedHostConnectionPoolHttps[RequestIdentifier]("android.googleapis.com", 443)
    val wnsPoolClientFlow = Http().cachedHostConnectionPoolHttps[wnsResponse]("hk2.notify.windows.com")

    val credentials = KeyChainManager.getGoogleCredential("ConnektSampleApp").get

    val httpDispatcher = new HttpPrepare[GCMPayloadEnvelope](
      new URL("https", "android.googleapis.com", 443, "/gcm/send"),
      HttpMethods.POST,
      scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + credentials.apiKey)),
      (envelope: GCMPayloadEnvelope) => HttpEntity(ContentTypes.`application/json`, envelope.gcmPayload.getJson),
      (envelope: GCMPayloadEnvelope) => RequestIdentifier(envelope.messageId, envelope.deviceId, envelope.appName)
    )

    val render = b.add(new RenderFlow)
    val fmtAndroid = b.add(new AndroidChannelFormatter)
    val fmtWindows = b.add(new WindowsChannelFormatter)
    val fmtIOS = b.add(new IOSChannelFormatter)
    val rHandlerGCM = b.add(new GCMResponseHandler)
    val platformPartition = b.add(new Partition[ConnektRequest](3, {
      case ios if "ios".equals(ios.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.WORKERS).debug(s"Routing IOS message: ${ios.id}")
        0
      case android if "android".equals(android.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.WORKERS).debug(s"Routing ANDROID message: ${android.id}")
        1
      case windows if "windows".equals(windows.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.WORKERS).debug(s"Routing WINDOWS message: ${windows.id}")
        2
    }))

    val merger = b.add(Merge[PNCallbackEvent](3))
    val wnsDispatcher = b.add(new WNSDispatcher())
    val apnsDispatcher = b.add(new APNSDispatcher(KeyChainManager.getAppleCredentials("retailapp").get))
    val wnsRHandler = b.add(new WNSResponseHandler)
    val gcmPoolFlow = b.add(gcmPoolClientFlow)
    val wnsPoolFlow = b.add(wnsPoolClientFlow)
    val gcmHttpDispatcher = b.add(httpDispatcher)

    val apnsEventCreator = b.add(Flow[Either[Throwable, String]].map {
      case Right(s) =>
        ConnektLogger(LogFile.WORKERS).info(s"apns event creator: $s")
        PNCallbackEvent("", "", s, "IOS", "", "", "", System.currentTimeMillis())
      case Left(x) =>
        ConnektLogger(LogFile.WORKERS).error(s"apns event creator: ${x.getMessage}")
        PNCallbackEvent("", "", x.getMessage, "IOS", "", "", "", System.currentTimeMillis())
    })

    render.out ~> platformPartition.in
    platformPartition.out(0) ~> fmtIOS ~> apnsDispatcher ~> apnsEventCreator ~> merger.in(0)
    platformPartition.out(1) ~> fmtAndroid ~> gcmHttpDispatcher ~> gcmPoolFlow ~> rHandlerGCM ~> merger.in(1)
    platformPartition.out(2) ~> fmtWindows ~> wnsDispatcher ~> wnsPoolFlow ~> wnsRHandler ~> merger.in(2)
    merger.out

    FlowShape(render.in, merger.out)
  })

  override def sink: Sink[PNCallbackEvent, NotUsed] = Sink.fromGraph(GraphDSL.create(){ implicit b =>

    val evtCreator = b.add(new PNBigfootEventCreator)
    evtCreator.out ~> Sink.ignore

    SinkShape(evtCreator.in)
  })

  override def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
  }
}
