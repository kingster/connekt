package com.flipkart.connekt.busybees.streams

import java.net.URL

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpDispatcher
import com.flipkart.connekt.busybees.streams.flows.{AndroidChannelDispatchFlow, RenderFlow}
import com.flipkart.connekt.busybees.streams.sources.{RateControl, KafkaSource}
import com.flipkart.connekt.commons.entities.Credentials
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels.{GCMPayload, ConnektRequest}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
object Topology {

  def bootstrap(consumerHelper: KafkaConsumerHelper) = {
    /* Fetch inlet / kafka message topic names */
    val topics = ConnektConfig.getStringList("allowedPNTopics").getOrElse(List.empty)
    topics.foreach(t => {
      val payloadFramer = (g: GCMPayload) => {}
      val httpDispatcher = new HttpDispatcher[GCMPayload](
        new URL("https", "android.googleapis.com", 443,"/gcm/send"),
        HttpMethods.POST,
        scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", Credentials.sampleAppCred), RawHeader("Content-Type", "application/json")),
        (g: GCMPayload) => HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), g.getJson)
      )

      Source.fromGraph(new KafkaSource[ConnektRequest](consumerHelper, t))
        .via(new RateControl[ConnektRequest](5, 1, 5))
        .via(new RenderFlow)
        .via(new AndroidChannelDispatchFlow)
        .via(httpDispatcher)
    })

    /* Start kafkaSource(s) for each topic */
    /* Attach rate-limiter flow for client sla */
    /* Wire PN dispatcher flows to sources */
  }

  def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
  }
}
