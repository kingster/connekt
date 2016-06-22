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

class HttpSink(subscription: Subscription, topologyShutdownTrigger: Promise[String])(implicit retryLimit:Int, am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) {

  val url = new URL(subscription.relayPoint.asInstanceOf[HTTPRelayPoint].url)
  val httpCachedClient = Http().cachedHostConnectionPool[HttpCallbackTracker](url.getHost, url.getPort)
  var currentShutdown = 0
  val shutdownThreshold= subscription.shutdownThreshold


  def getHttpSink(): Sink[(HttpRequest, HttpCallbackTracker), NotUsed] = {

    val responseResultHandler = new ResponseResultHandler(url.getPath)

    Sink.fromGraph(GraphDSL.create() { implicit b =>
      val mergePref = b.add(MergePreferred[(HttpRequest,HttpCallbackTracker)](1))
      val resultHandler = b.add(responseResultHandler)

      mergePref.out ~> httpCachedClient.map(responseEvaluator) ~> resultHandler.in
      resultHandler.out0 ~> mergePref.preferred
      resultHandler.out1 ~> Sink.foreach(println)
      resultHandler.out2 ~> Sink.foreach(println)

      SinkShape(mergePref.in(0))
    })
  }

  private def responseEvaluator(responseObject: (Try[HttpResponse],HttpCallbackTracker)) : Either[HttpResponse, HttpCallbackTracker] = {
    currentShutdown = currentShutdown + 1
    val resp = responseObject._1
    val httpCallbackTracker = responseObject._2
    resp match {
      case Success(response) =>
        resp.get.entity.dataBytes.runWith(Sink.ignore)
        response.status.intValue() match {
          case 200 =>
            currentShutdown = 0
            Left(response)
          case _ =>
            if( currentShutdown > shutdownThreshold && !topologyShutdownTrigger.isCompleted) topologyShutdownTrigger.success("Too many error from server")
            if(httpCallbackTracker.error == retryLimit) Right(new HttpCallbackTracker(httpCallbackTracker.payload, httpCallbackTracker.error+1, true))
            else Right (new HttpCallbackTracker(httpCallbackTracker.payload, httpCallbackTracker.error+1, false))
        }
      case Failure(e) =>
        if( currentShutdown > shutdownThreshold && !topologyShutdownTrigger.isCompleted) topologyShutdownTrigger.success("Too many error from server")
        if(httpCallbackTracker.error == retryLimit) Right(new HttpCallbackTracker(httpCallbackTracker.payload, httpCallbackTracker.error+1, true))
        else Right (new HttpCallbackTracker(httpCallbackTracker.payload, httpCallbackTracker.error+1, false))
    }
  }

}
