package com.flipkart.connekt.busybees.streams

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, SinkShape, SourceShape}
import akka.util.{ByteString, ByteStringBuilder}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{APNSDispatcher, HttpPrepare, WNSDispatcher}
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters.{AndroidChannelFormatter, IOSChannelFormatter, WindowsChannelFormatter}
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.{GCMResponseHandler, WNSResponseHandler}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, GCMPayload, PNCallbackEvent, PNRequestInfo}
import com.flipkart.connekt.commons.services.{ConnektConfig, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */

case class wnsResponse(appName: String, requestId: String)

object Topology {

  def bootstrap(consumerHelper: KafkaConsumerHelper) = {

    implicit val system = BusyBeesBoot.system
    implicit val ec = BusyBeesBoot.system.dispatcher
    implicit val mat = ActorMaterializer()

    /*##############################################################
    * GRAPH TEMPLATE DEFINITION
    ##############################################################*/
    //this would need to change to dynamic based on which app this is being send for.
    val credentials = KeyChainManager.getGoogleCredential("ConnektSampleApp").get

    val httpDispatcher = new HttpPrepare[GCMPayload](
      new URL("https", "android.googleapis.com", 443, "/gcm/send"),
      HttpMethods.POST,
      scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + credentials.apiKey)),
      (payload: GCMPayload) => HttpEntity(ContentTypes.`application/json`, payload.getJson)
    )

    val loggerSink = Sink.foreachParallel[(Try[HttpResponse], String)](10)(tR => {
      tR._1 match {
        case Success(r) =>
          ConnektLogger(LogFile.PROCESSORS).info(s"Sink:: Received httpResponse for r: ${tR._2}")
          r.entity.dataBytes.runFold[ByteStringBuilder](ByteString.newBuilder)((u, bs) => {u ++= bs}).onComplete {
            case Success(b) =>
              ConnektLogger(LogFile.PROCESSORS).info(s"LoggingSink:: ResponseBody:: ${b.result().decodeString("UTF-8")}")
            case Failure(e1) =>
              ConnektLogger(LogFile.PROCESSORS).error(s"LoggingSink:: Error Processing ResponseBody:: ${e1.getMessage}", e1)
          }
        case Failure(e2) =>
          ConnektLogger(LogFile.PROCESSORS).error(s"Sink:: Received Error for r: ${tR._2}, e: ${e2.getMessage}", e2)
      }
    })

    val gcmPoolClientFlow = Http().cachedHostConnectionPoolHttps[String]("android.googleapis.com", 443)

    val wnsPoolClientFlow = Http().cachedHostConnectionPoolHttps[wnsResponse]("hk2.notify.windows.com")

    //    lazy implicit val wnsPoolClientFlow = Http().cachedHostConnectionPoolHttps[wnsResponse]("hk2.notify.windows.com")

    /* Composite source of all kafka channel-specific topics */
    val compositeSource = Source.fromGraph(GraphDSL.create(){ implicit b =>
      /* Excluding the bitch who's spoiling the composite-source party since Friday */
      val topics = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.filterNot(_.equalsIgnoreCase("push_ec06191e4ed41b85b45973652c659ad4704644a13975a088c731bf69375ac5c1"))
      ConnektLogger(LogFile.PROCESSORS).info(s"Creating composite source for topics: ${topics.toString()}")

      val merge = b.add(Merge[ConnektRequest](topics.size))
      for(portNum <- 0 to merge.n - 1) {
        new KafkaSource[ConnektRequest](consumerHelper, topic = topics(portNum)) ~> merge.in(portNum)
      }

      SourceShape(merge.out)
    })

    /* Start kafkaSource(s) for each topic */
    /* Attach rate-limiter flow for client sla */
    /* Wire PN dispatcher flows to sources */
    val sink = Sink.fromGraph(GraphDSL.create(){ implicit b =>

//      val source = b.add(new KafkaSource[ConnektRequest](consumerHelper, "PN_connekt"))
      //      val flowRate = b.add(new RateControl[ConnektRequest](2, 1, 2))
      val render = b.add(new RenderFlow)
      val fmtAndroid = b.add(new AndroidChannelFormatter)
      val fmtWindows = b.add(new WindowsChannelFormatter)
      val fmtIOS = b.add(new IOSChannelFormatter)
      val rHandlerGCM = b.add(new GCMResponseHandler)
      val evtCreator = b.add( new PNBigfootEventCreator)
      //      val evtSenderSink = b.add(new EventSenderSink)
      val platformPartition = b.add(new Partition[ConnektRequest](3, {
        case ios if "ios".equals(ios.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
          ConnektLogger(LogFile.WORKERS).info(s"Routing IOS message: ${ios.id}")
          0
        case android if "android".equals(android.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
          ConnektLogger(LogFile.WORKERS).info(s"Routing ANDROID message: ${android.id}")
          1
        case windows if "windows".equals(windows.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
          ConnektLogger(LogFile.WORKERS).info(s"Routing WINDOWS message: ${windows.id}")
          2
      }))

      val merger = b.add(Merge[PNCallbackEvent](3))
      val wnsDispatcher = b.add(new WNSDispatcher())
      val apnsDispatcher = b.add(new APNSDispatcher(KeyChainManager.getAppleCredentials("retailapp").get))
      val wnsRHandler = b.add(new WNSResponseHandler)
      val gcmPoolFlow = b.add(gcmPoolClientFlow)
      val wnsPoolFlow = b.add(wnsPoolClientFlow)

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
      platformPartition.out(1) ~> fmtAndroid ~> httpDispatcher ~> gcmPoolFlow ~> rHandlerGCM ~> merger.in(1)
      platformPartition.out(2) ~> fmtWindows ~> wnsDispatcher ~> wnsPoolFlow ~> wnsRHandler ~> merger.in(2)
      merger.out ~> evtCreator ~> Sink.ignore

      SinkShape(render.in)
    })

    //Run the entire flow
    compositeSource.runWith(sink)
    ConnektLogger(LogFile.PROCESSORS).info(s"######## Started the runnable graph ########")

    Thread.sleep(250000)


    /* start all pn flows */
    //    channelTopics.filter(_.startsWith(pnTopicPrefix)).foreach(t => {
    //      ConnektLogger(LogFile.WORKERS).info(s"Bootstrapping flow for topic: $t")
    //      RunnableGraph.fromGraph(g).run()
    //
    //      Source.fromGraph(new KafkaSource[ConnektRequest](consumerHelper, t))
    //        .via(new RateControl[ConnektRequest](2, 1, 2))
    //        .via(new RenderFlow)
    //        .via(new AndroidChannelFormatter)
    //        .via(httpDispatcher)
    //        .via(poolClientFlow)
    //        .via(new GCMResponseHandler)
    //        .via(new PNBigfootEventCreator)
    //        .runWith(new EventSenderSink)
    //    })
  }

  def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
  }
}
