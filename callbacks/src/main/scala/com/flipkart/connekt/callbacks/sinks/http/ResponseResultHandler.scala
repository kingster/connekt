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
import akka.stream.{Attributes, FanOutShape3, Inlet, Outlet}
import scala.concurrent.ExecutionContext

class ResponseResultHandler(url: String)(implicit val ec: ExecutionContext) extends GraphStage[FanOutShape3[Either[HttpResponse,HttpCallbackTracker],(HttpRequest,HttpCallbackTracker), HttpResponse,HttpCallbackTracker]] {

  val in = Inlet[Either[HttpResponse, HttpCallbackTracker]]("input")
  val retryOutlet = Outlet[(HttpRequest, HttpCallbackTracker)]("error.out")
  val success = Outlet[HttpResponse]("success.out")
  val discardOutlet = Outlet[HttpCallbackTracker]("discardedEvents.out")

  override def shape = new FanOutShape3(in, retryOutlet, success, discardOutlet)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val result = grab(in)
        result match {
          case Left(httpResponse) => push(success, httpResponse)
          case Right(httpCallbackTracker) => if (httpCallbackTracker.discarded) push(discardOutlet, httpCallbackTracker)
          else {
            val httpEntity = HttpEntity(ContentTypes.`application/json`, httpCallbackTracker.payload)
            val httpRequest = HttpRequest(method = HttpMethods.POST, uri = url, entity = httpEntity)
            push(retryOutlet, (httpRequest, httpCallbackTracker))
          }
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

