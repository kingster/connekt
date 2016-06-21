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
import akka.stream.scaladsl.{Flow, GraphDSL, MergePreferred, Sink}
import akka.stream.{ActorMaterializer, SinkShape}
import com.flipkart.connekt.commons.entities.{HTTPRelayPoint, Subscription}
import akka.stream.scaladsl.GraphDSL.Implicits._

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

class HttpSink(subscription: Subscription, topologyShutdownTrigger: Promise[String])(implicit retryLimit:Int, am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) {

  val url = new URL(subscription.relayPoint.asInstanceOf[HTTPRelayPoint].url)
  val httpCachedClient = Http().cachedHostConnectionPool[HttpCallbackTracker](url.getHost, url.getPort)
  var currentShutdown = 0
  val shutdownThreshold= subscription.shutdownThreshold


  def getHttpSink(): Sink[(HttpRequest, HttpCallbackTracker), NotUsed] = {

    val responseResultHandler = new ResponseResultHandler(url.getPath)

    Sink.fromGraph(GraphDSL.create() { implicit b =>
      val mergePreferredStaged = b.add(MergePreferred[(HttpRequest,HttpCallbackTracker)](1))
      val resultHandler = b.add(responseResultHandler)

      mergePreferredStaged.out ~> httpCachedClient.map(responseEvaluator) ~> resultHandler.in
      resultHandler.out0 ~> mergePreferredStaged.preferred
      resultHandler.out1 ~> Sink.foreach(println)
      resultHandler.out2 ~> Sink.foreach(println)

      SinkShape(mergePreferredStaged.in(0))
    })
  }

  private def responseEvaluator(responseObject: (Try[HttpResponse],HttpCallbackTracker)) : Either[HttpResponse, HttpCallbackTracker] = {
    currentShutdown = currentShutdown + 1
    val response = responseObject._1
    val callbackTracker = responseObject._2
    response match {
      case Success(r) =>
        response.get.entity.dataBytes.runWith(Sink.ignore)
        r.status.intValue() match {
          case 200 =>
            currentShutdown = 0
            Left(response.get)
          case _ =>
            if( currentShutdown > shutdownThreshold && !topologyShutdownTrigger.isCompleted) topologyShutdownTrigger.success("Too many error from server")
            if(callbackTracker.error == retryLimit) Right(new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, true))
            else Right (new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, false))
        }
      case Failure(e) =>
        if( currentShutdown > shutdownThreshold && !topologyShutdownTrigger.isCompleted) topologyShutdownTrigger.success("Too many error from server")
        if(callbackTracker.error == retryLimit) Right(new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, true))
        else Right (new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, false))
    }
  }

}
