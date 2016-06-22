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

import akka.http.scaladsl.model.{HttpEntity, _}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class ResponseHandler(url: String)(implicit val ec: ExecutionContext) extends GraphStage[UniformFanOutShape[(Try[HttpResponse], HttpCallbackTracker),(HttpRequest,HttpCallbackTracker)]] {

  val in = Inlet[(Try[HttpResponse], HttpCallbackTracker)]("input")

  val retryOutlet = Outlet[(HttpRequest,HttpCallbackTracker)]("error.out")
  val success = Outlet[(HttpRequest,HttpCallbackTracker)]("success.out")
  val discardOutlet = Outlet[(HttpRequest,HttpCallbackTracker)]("discardedEvents.out")

  override def shape = new UniformFanOutShape(in, Array(retryOutlet, success, discardOutlet))

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val result = grab(in)
        val httpRequest = HttpRequest(method = HttpMethods.POST, uri = url, entity = HttpEntity(ContentTypes.`application/json`, result._2.payload))
        val httpCallbackTracker = result._2

        result._1 match {
          case Success(response) =>
            response.status.intValue() match {
              case 200 =>
                push(success, (httpRequest, httpCallbackTracker))
              case _ =>
                if (httpCallbackTracker.discarded) push(discardOutlet, (httpRequest, httpCallbackTracker))
                else push(retryOutlet, (httpRequest, httpCallbackTracker))
            }
          case Failure(e) =>
            if (httpCallbackTracker.discarded) push(discardOutlet, (httpRequest, httpCallbackTracker))
            else push(retryOutlet, (httpRequest, httpCallbackTracker))
        }

      }
    })


    setHandler(success, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

    setHandler(retryOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

    setHandler(discardOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

  }
}

