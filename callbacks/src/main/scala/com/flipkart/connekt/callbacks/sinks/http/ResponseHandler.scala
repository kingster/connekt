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

import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.Sink
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class ResponseHandler(implicit mat:ActorMaterializer,  ec: ExecutionContext) extends GraphStage[UniformFanOutShape[(Try[HttpResponse], HttpCallbackTracker),(HttpRequest,HttpCallbackTracker)]] {

  val in = Inlet[(Try[HttpResponse], HttpCallbackTracker)]("input")

  val retryOnErrorOutlet = Outlet[(HttpRequest,HttpCallbackTracker)]("retryOnError.out")
  val successOutlet = Outlet[(HttpRequest,HttpCallbackTracker)]("success.out")
  val discardOutlet = Outlet[(HttpRequest,HttpCallbackTracker)]("discard.out")

  override def shape = new UniformFanOutShape(in, Array(retryOnErrorOutlet, successOutlet, discardOutlet))

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val result = grab(in)
        val httpCallbackTracker = result._2

        result._1 match {
          case Success(response) =>
            response.entity.dataBytes.runWith(Sink.ignore)
            response.status.intValue() match {
              case s if s/100 == 2 =>
                push(successOutlet, (httpCallbackTracker.httpRequest, httpCallbackTracker))
              case _ =>
                if (httpCallbackTracker.discarded)
                  push(discardOutlet, (httpCallbackTracker.httpRequest, httpCallbackTracker))
                else
                  push(retryOnErrorOutlet, (httpCallbackTracker.httpRequest, httpCallbackTracker))
            }
          case Failure(e) =>
            if (httpCallbackTracker.discarded)
              push(discardOutlet, (httpCallbackTracker.httpRequest, httpCallbackTracker))
            else
              push(retryOnErrorOutlet, (httpCallbackTracker.httpRequest, httpCallbackTracker))
        }

      }
    })

    setHandler(retryOnErrorOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })
    
    setHandler(successOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

    setHandler(discardOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

  }
}

