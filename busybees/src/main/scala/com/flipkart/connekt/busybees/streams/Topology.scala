package com.flipkart.connekt.busybees.streams

import java.net.URL

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpDispatcher
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.formaters.AndroidChannelFormatter
import com.flipkart.connekt.busybees.streams.sinks.LoggingSink
import com.flipkart.connekt.busybees.streams.sources.{KafkaSource, RateControl}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, GCMPayload}
import com.flipkart.connekt.commons.services.{ConnektConfig, CredentialManager}
import com.flipkart.connekt.commons.utils.StringUtils._

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
    val topics = ConnektConfig.getStringList("allowedPNTopics").getOrElse(List.empty)

    topics.foreach(t => {

      ConnektLogger(LogFile.WORKERS).info(s"Bootstrapping flow for topic: $t")

      //this would need to change to dynamic based on which app this is being send for.
      val credentials = CredentialManager.getCredential("PN.ConnektSampleApp")

      val httpDispatcher = new HttpDispatcher[GCMPayload](
        new URL("https", "android.googleapis.com", 443,"/gcm/send"),
        HttpMethods.POST,
        scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + credentials.password)),
        (payload: GCMPayload) => HttpEntity(ContentTypes.`application/json`, payload.getJson)
      )

      /* Start kafkaSource(s) for each topic */
      /* Attach rate-limiter flow for client sla */
      /* Wire PN dispatcher flows to sources */

      Source.fromGraph(new KafkaSource[ConnektRequest](consumerHelper, t))
        .via(new RateControl[ConnektRequest](5, 1, 5))
        .via(new RenderFlow)
        .via(new AndroidChannelFormatter)
        .via(httpDispatcher)
        .runWith(new LoggingSink)
    })

  }

  def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
  }
}
