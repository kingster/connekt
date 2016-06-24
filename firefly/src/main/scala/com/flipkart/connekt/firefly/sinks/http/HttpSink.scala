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


import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, MergePreferred, Sink}
import akka.stream.{ActorMaterializer, SinkShape}
import com.flipkart.connekt.commons.entities.{HTTPEventSink, Subscription}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.{ExecutionContext, Promise}

class HttpSink(subscription: Subscription, retryLimit: Int, topologyShutdownTrigger: Promise[String])(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) {

  val httpCachedClient = Http().superPool[HttpCallbackTracker]()
  val consecutiveSinkFailures = new AtomicInteger(0)

  def getHttpSink: Sink[CallbackEvent, NotUsed] = {

    Sink.fromGraph(GraphDSL.create() { implicit b =>
      val httpResponseHandler = b.add(new ResponseHandler(retryLimit, subscription.shutdownThreshold, topologyShutdownTrigger))
      val event2HttpRequestMapper = b.add(Flow[CallbackEvent].map(httpPrepare))
      val httpRequestMergePref = b.add(MergePreferred[(HttpRequest, HttpCallbackTracker)](1))

      event2HttpRequestMapper ~> httpRequestMergePref.in(0)
      httpRequestMergePref.out ~> httpCachedClient ~> httpResponseHandler.in
      httpResponseHandler.out(0) ~> httpRequestMergePref.preferred
      httpResponseHandler.out(1) ~> Sink.foreach[(HttpRequest,HttpCallbackTracker)] { event =>
          ConnektLogger(LogFile.SERVICE).debug(s"HttpSink message delivered: $event")
      }

      httpResponseHandler.out(2) ~> Sink.foreach[(HttpRequest,HttpCallbackTracker)] { event =>
        ConnektLogger(LogFile.SERVICE).warn(s"HttpSink message discarded: $event")
      }

      SinkShape(event2HttpRequestMapper.in)
    })
  }

  private def httpPrepare(event: CallbackEvent): (HttpRequest, HttpCallbackTracker) = {

    val httpEntity = HttpEntity(ContentTypes.`application/json`, event.getJson)
    val endpointDetail = subscription.eventSink.asInstanceOf[HTTPEventSink]
    val httpRequest = HttpRequest(method = HttpMethods.getForKey(endpointDetail.method.toUpperCase).get, uri = subscription.eventSink.asInstanceOf[HTTPEventSink].url, entity = httpEntity)
    val callbackTracker = HttpCallbackTracker(httpRequest)

    (httpRequest, callbackTracker)
  }
}
