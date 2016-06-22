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

import java.net.URL

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{GraphDSL, MergePreferred, Sink}
import akka.stream.{ActorMaterializer, SinkShape}
import com.flipkart.connekt.commons.entities.{HTTPRelayPoint, Subscription}
import akka.stream.scaladsl.GraphDSL.Implicits._

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

class HttpSink(subscription: Subscription, retryLimit: Int, topologyShutdownTrigger: Promise[String])(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) {

  val url = new URL(subscription.relayPoint.asInstanceOf[HTTPRelayPoint].url)
  val httpCachedClient = Http().superPool[HttpCallbackTracker]()
  var serverFailure = 0
  val shutdownThreshold = subscription.shutdownThreshold

  def getHttpSink(): Sink[(HttpRequest, HttpCallbackTracker), NotUsed] = {

    val responseHandler = new ResponseHandler(url.getPath)

    Sink.fromGraph(GraphDSL.create() { implicit b =>
      val mergePref = b.add(MergePreferred[(HttpRequest,HttpCallbackTracker)](1))
      val resultHandler = b.add(responseHandler)

      mergePref.out ~> httpCachedClient.map(updateTracker) ~> resultHandler.in
      resultHandler.out(0) ~> mergePref.preferred
      resultHandler.out(1) ~> Sink.foreach(println)
      resultHandler.out(2) ~> Sink.foreach(println) //TODO: might be used for sidelinig later

      SinkShape(mergePref.in(0))
    })
  }

  private def updateTracker(responseResult: (Try[HttpResponse], HttpCallbackTracker)): (Try[HttpResponse], HttpCallbackTracker) = {

    serverFailure = serverFailure + 1
    val httpResponse = responseResult._1
    val tracker = responseResult._2

    def trackerFailureUdate: (Try[HttpResponse], HttpCallbackTracker) = {
      if (serverFailure > shutdownThreshold && !topologyShutdownTrigger.isCompleted) topologyShutdownTrigger.success("Too many error from server")
      if (tracker.failureCount == retryLimit) (httpResponse, HttpCallbackTracker(tracker.payload, tracker.failureCount + 1, true))
      else (httpResponse, HttpCallbackTracker(tracker.payload, tracker.failureCount + 1))
    }

    httpResponse match {
      case Success(response) =>
        response.status.intValue() match {
          case 200 =>
            serverFailure = 0
            responseResult
          case _ => trackerFailureUdate
        }
      case Failure(e) => trackerFailureUdate
    }
  }

}
