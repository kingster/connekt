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
package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URI


import akka.http.scaladsl.model.{HttpMethods, HttpEntity, HttpHeader, HttpRequest}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.WNSPayloadEnvelope
import com.flipkart.connekt.commons.services.WindowsOAuthService

/**
 * @author aman.shrivastava on 08/02/16.
 */
class WNSDispatcherPrepare extends GraphStage[FlowShape[WNSPayloadEnvelope, (HttpRequest, WNSRequestTracker)]] {

  val in = Inlet[WNSPayloadEnvelope]("WNSDispatcher.In")
  val out = Outlet[(HttpRequest, WNSRequestTracker)]("WNSDispatcher.Out")

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val message = grab(in)

        try {
          ConnektLogger(LogFile.PROCESSORS).debug(s"WNSDispatcher:: ON_PUSH for ${message.messageId}")
          ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPush:: Received Message: $message")

          val uri = new URI(message.token).toURL
          val bearerToken = WindowsOAuthService.getToken(message.appName).map(_.token).getOrElse("INVALID")
          val headers = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "Bearer " + bearerToken), RawHeader("X-WNS-Type", message.wnsPayload.getType))

          val payload = HttpEntity(message.wnsPayload.getContentType, message.wnsPayload.getBody)
          val request = new HttpRequest(HttpMethods.POST, uri.getFile, headers, payload)

          if(isAvailable(out)) {
            push(out, (request, WNSRequestTracker(message.appName, message.messageId, message)))
            ConnektLogger(LogFile.PROCESSORS).debug(s"WNSDispatcher:: PUSHED downstream for ${message.messageId}")
          }

        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"WNSDispatcher:: onPush :: Error", e)
            if(!hasBeenPulled(in)) {
              ConnektLogger(LogFile.PROCESSORS).debug(s"WNSDispatcher:: PUSHED downstream for ${message.messageId}")
              pull(in)
            }
        }
      }

      override def onUpstreamFinish(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info("WNSDispatcher:: onUpstream finish invoked")
        super.onUpstreamFinish()
      }

      override def onUpstreamFailure(e: Throwable): Unit = {
        ConnektLogger(LogFile.PROCESSORS).error(s"WNSDispatcher:: onUpstream failure: ${e.getMessage}", e)
        super.onUpstreamFinish()
      }

    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPull")
        if(!hasBeenPulled(in)) {
          ConnektLogger(LogFile.PROCESSORS).debug(s"WNSDispatcher:: PULLED upstream on downstream pull")
          pull(in)
        }
      }
    })


    override def afterPostStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: postStop")

      //apnsClient.stop()
      super.afterPostStop()
    }

  }
}
