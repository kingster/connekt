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

import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.Sink
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

class HttpResponseHandler(retryLimit: Int, shutdownThreshold: Int, topologyShutdownTrigger: Promise[String])(implicit mat:ActorMaterializer,  ec: ExecutionContext)
  extends GraphStage[UniformFanOutShape[(Try[HttpResponse], HttpRequestTracker),(HttpRequest,HttpRequestTracker)]] {

  val in = Inlet[(Try[HttpResponse], HttpRequestTracker)]("input")
  val retryOnErrorOut = Outlet[(HttpRequest, HttpRequestTracker)]("retryOnError.out")
  val successOut = Outlet[(HttpRequest, HttpRequestTracker)]("success.out")
  val discardOut = Outlet[(HttpRequest, HttpRequestTracker)]("discard.out")

  override def shape = new UniformFanOutShape(in, Array(retryOnErrorOut, successOut, discardOut))

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    val consecutiveSendFailures = new AtomicInteger(0)
    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val result = grab(in)
        val httpCallbackTracker = result._2
        
        result._1 match {
          case Success(response) =>
            response.entity.dataBytes.runWith(Sink.ignore)
            response.status.intValue() match {
              case s if s/100 == 2 =>
                consecutiveSendFailures.set(0)
                push(successOut, (httpCallbackTracker.httpRequest, httpCallbackTracker))
              case e =>
                ConnektLogger(LogFile.SERVICE).error(s"Callback event relay non-200 code: $e")
                consecutiveSendFailures.incrementAndGet()
                push(if(httpCallbackTracker.failureCount > retryLimit) discardOut else retryOnErrorOut, (httpCallbackTracker.httpRequest, httpCallbackTracker.copy(failureCount = 1 + httpCallbackTracker.failureCount)))
            }
          case Failure(e) =>
            ConnektLogger(LogFile.SERVICE).error(s"Callback event relay failure", e)
            consecutiveSendFailures.incrementAndGet()
            push(if(httpCallbackTracker.failureCount > retryLimit) discardOut else retryOnErrorOut, (httpCallbackTracker.httpRequest, httpCallbackTracker.copy(failureCount = 1 + httpCallbackTracker.failureCount)))
        }
        
        if(consecutiveSendFailures.get() > shutdownThreshold) {
          ConnektLogger(LogFile.SERVICE).info("Client callback topology shutdown trigger executed on threshold failures")
          topologyShutdownTrigger.complete(Success("Client callback topology shutdown trigger executed on threshold failures"))
        }
      }
    })

    setHandler(retryOnErrorOut, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })
    
    setHandler(successOut, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

    setHandler(discardOut, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })
  }
}

