package com.flipkart.connekt.busybees.streams

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.{ByteString, ByteStringBuilder}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpPrepare
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters.AndroidChannelFormatter
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.GCMResponseHandler
import com.flipkart.connekt.busybees.streams.sinks.EventSenderSink
import com.flipkart.connekt.busybees.streams.sources.{KafkaSource, RateControl}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, GCMPayload}
import com.flipkart.connekt.commons.services.{ConnektConfig, CredentialManager}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
object Topology {

  def bootstrap(consumerHelper: KafkaConsumerHelper) = {

    implicit val system = BusyBeesBoot.system
    implicit val ec = BusyBeesBoot.system.dispatcher
    implicit val mat = ActorMaterializer()


    /* Fetch inlet / kafka message topic names */
    val topics = ConnektConfig.getList[String]("allowedPNTopics")

    topics.foreach(t => {

      ConnektLogger(LogFile.WORKERS).info(s"Bootstrapping flow for topic: $t")

      //this would need to change to dynamic based on which app this is being send for.
      val credentials = CredentialManager.getCredential("PN.ConnektSampleApp")

      val httpDispatcher = new HttpPrepare[GCMPayload](
        new URL("https", "android.googleapis.com", 443,"/gcm/send"),
        HttpMethods.POST,
        scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + credentials.password)),
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

      lazy implicit val poolClientFlow = Http().cachedHostConnectionPoolHttps[String]("android.googleapis.com", 443)

      /* Start kafkaSource(s) for each topic */
      /* Attach rate-limiter flow for client sla */
      /* Wire PN dispatcher flows to sources */

      Source.fromGraph(new KafkaSource[ConnektRequest](consumerHelper, t))
        .via(new RateControl[ConnektRequest](2, 1, 2))
        .via(new RenderFlow)
        .via(new AndroidChannelFormatter)
        .via(httpDispatcher)
        .via(poolClientFlow)
        .via(new GCMResponseHandler)
        .via(new PNBigfootEventCreator)
        .runWith(new EventSenderSink)
    })

  }

  def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
  }
}
