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
package com.flipkart.connekt.callbacks.sinks.http


import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, GraphDSL, MergePreferred, Sink}
import akka.stream.{ActorMaterializer, SinkShape}
import com.flipkart.connekt.commons.entities.{HTTPEventSink, Subscription}
import akka.stream.scaladsl.GraphDSL.Implicits._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.utils.StringUtils._
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

class HttpSink(subscription: Subscription, retryLimit: Int, topologyShutdownTrigger: Promise[String])(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) {

  val httpCachedClient = Http().superPool[HttpCallbackTracker]()
  var serverFailure = 0
  val shutdownThreshold = subscription.shutdownThreshold

  def getHttpSink: Sink[CallbackEvent, NotUsed] = {

    Sink.fromGraph(GraphDSL.create() { implicit b =>
      val httpResponseHandler = b.add(new ResponseHandler())
      val event2HttpRequestMapper = b.add(Flow[CallbackEvent].map(httpPrepare))
      val httpRequestMergePref = b.add(MergePreferred[(HttpRequest, HttpCallbackTracker)](1))

      event2HttpRequestMapper ~> httpRequestMergePref.in(0)
      httpRequestMergePref.out ~> httpCachedClient.map(updateTracker) ~> httpResponseHandler.in
      httpResponseHandler.out(0) ~> httpRequestMergePref.preferred
      httpResponseHandler.out(1) ~> Sink.foreach[(HttpRequest,HttpCallbackTracker)] { event =>
          ConnektLogger(LogFile.SERVICE).debug(s"HttpSink message delivered: $event")
      }

      httpResponseHandler.out(2) ~> Sink.foreach[(HttpRequest,HttpCallbackTracker)] { event =>
        ConnektLogger(LogFile.SERVICE).debug(s"HttpSink message delivered: $event")
      }

      SinkShape(event2HttpRequestMapper.in)
    })
  }

  private def updateTracker(responseResult: (Try[HttpResponse], HttpCallbackTracker)): (Try[HttpResponse], HttpCallbackTracker) = {

    serverFailure = serverFailure + 1
    val httpResponse = responseResult._1
    val tracker = responseResult._2

    def trackerFailureUpdate: (Try[HttpResponse], HttpCallbackTracker) = {
      if (serverFailure > shutdownThreshold && !topologyShutdownTrigger.isCompleted) topologyShutdownTrigger.success("Too many error from server")
      if (tracker.failureCount == retryLimit)
        httpResponse -> HttpCallbackTracker(tracker.httpRequest, tracker.failureCount + 1, discarded = true)
      else
        httpResponse -> HttpCallbackTracker(tracker.httpRequest, tracker.failureCount + 1)
    }

    httpResponse match {
      case Success(response) =>
        response.status.intValue() match {
          case 200 =>
            serverFailure = 0
            responseResult
          case _ => trackerFailureUpdate
        }
      case Failure(e) => trackerFailureUpdate
    }
  }

  private def httpPrepare(event: CallbackEvent): (HttpRequest, HttpCallbackTracker) = {
    val httpEntity = HttpEntity(ContentTypes.`application/json`, event.getJson)
    val endpointDetail = subscription.sink.asInstanceOf[HTTPEventSink]
    val httpRequest = HttpRequest(method = HttpMethods.getForKey(endpointDetail.method.toUpperCase).get, uri = subscription.sink.asInstanceOf[HTTPEventSink].url, entity = httpEntity)
    val callbackTracker = HttpCallbackTracker(httpRequest)
    (httpRequest, callbackTracker)
  }
}
