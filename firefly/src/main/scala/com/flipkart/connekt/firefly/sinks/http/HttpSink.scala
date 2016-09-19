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
package com.flipkart.connekt.firefly.sinks.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, MergePreferred, Sink}
import akka.stream.{ActorMaterializer, SinkShape}
import com.flipkart.connekt.commons.entities.{HTTPEventSink, Subscription, SubscriptionEvent}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.firefly.dispatcher.HttpDispatcher

import scala.collection._
import scala.concurrent.{ExecutionContext, Promise}

class HttpSink(subscription: Subscription, retryLimit: Int, topologyShutdownTrigger: Promise[String])(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) extends Instrumented {

  val httpCachedClient = HttpDispatcher.httpFlow

  def getHttpSink: Sink[SubscriptionEvent, NotUsed] = {

    Sink.fromGraph(GraphDSL.create() { implicit b =>
      val httpResponseHandler = b.add(new HttpResponseHandler(retryLimit, subscription.shutdownThreshold, subscription.id, topologyShutdownTrigger))
      val event2HttpRequestMapper = b.add(Flow[SubscriptionEvent].map(httpPrepare))
      val httpRequestMergePref = b.add(MergePreferred[(HttpRequest, HttpRequestTracker)](1))

      event2HttpRequestMapper ~> httpRequestMergePref.in(0)
      httpRequestMergePref.out ~> httpCachedClient ~> httpResponseHandler.in
      httpResponseHandler.out(0) ~> httpRequestMergePref.preferred
      httpResponseHandler.out(1) ~> Sink.foreach[(HttpRequest, HttpRequestTracker)] { event =>
        meter(s"firefly.http.${subscription.name}.success").mark()
        ConnektLogger(LogFile.SERVICE).debug(s"HttpSink message delivered: $event")
      }

      httpResponseHandler.out(2) ~> Sink.foreach[(HttpRequest, HttpRequestTracker)] { event =>
        meter(s"firefly.http.${subscription.name}.failure").mark()
        ConnektLogger(LogFile.SERVICE).warn(s"HttpSink message discarded: $event")
      }

      SinkShape(event2HttpRequestMapper.in)
    })
  }

  private def httpPrepare(event: SubscriptionEvent): (HttpRequest, HttpRequestTracker) = {

    val sink = subscription.sink.asInstanceOf[HTTPEventSink]

    val httpEntity = HttpEntity(ContentTypes.`application/json`, event.payload.toString)
    val httpRequest = event.header match {
      case null => HttpRequest(method = HttpMethods.getForKey(sink.method.toUpperCase).get, uri = sink.url, entity = httpEntity)
      case _ => HttpRequest(method = HttpMethods.getForKey(sink.method.toUpperCase).get, uri = sink.url, entity = httpEntity,
        headers = immutable.Seq[HttpHeader]( event.header.map { case (key, value) => RawHeader(key, value) }.toArray: _ *))
    }


//    ConnektLogger(LogFile.SERVICE).info(s"Preparing http : ${event.payload}, ${sink.url}, ${httpEntity}")

    val requestTracker = HttpRequestTracker(httpRequest)

    httpRequest -> requestTracker
  }

}
