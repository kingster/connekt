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

import java.net.URL

import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.GCMPayloadEnvelope
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._
 
class GCMDispatcherPrepare(uri: URL = new URL("https", "android.googleapis.com", 443, "/gcm/send"))
  extends GraphStage[FlowShape[GCMPayloadEnvelope, (HttpRequest, GCMRequestTracker)]] {

  val in = Inlet[GCMPayloadEnvelope]("GCMDispatcher.In")
  val out = Outlet[(HttpRequest, GCMRequestTracker)]("GCMDispatcher.Out")

  override def shape  = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val message = grab(in)
        try {
          ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: ON_PUSH for ${message.messageId}")
          ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: onPush:: Received Message: ${message.toString}")

          val requestEntity = HttpEntity(ContentTypes.`application/json`, message.gcmPayload.getJson)
          val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(message.appName).get.apiKey))
          val httpRequest = new HttpRequest(HttpMethods.POST, uri.getPath, requestHeaders, requestEntity)
          val requestTrace = GCMRequestTracker(message.messageId, message.deviceId, message.appName)

          ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: onPush:: Request Payload : ${httpRequest.entity.asInstanceOf[Strict].data.decodeString("UTF-8")}")
          ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: onPush:: Relayed (HttpRequest,requestTrace) to next stage for: ${requestTrace.messageId}")

          if(isAvailable(out)) {
            push(out, (httpRequest, requestTrace))
            ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: PUSHED downstream for ${message.messageId}")
          }

        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMDispatcherPrepare:: onPush :: ${e.getMessage}", e)
            if(!hasBeenPulled(in)) {
              pull(in)
              ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: PULLED upstream for ${message.messageId}")
            }
        }
      }

      override def onUpstreamFailure(e: Throwable): Unit = {
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMDispatcherPrepare:: onUpstream failure: ${e.getMessage}", e)
        super.onUpstreamFinish()
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"GCMDispatcherPrepare:: onPull")
        if(!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: PULLED upstream on downstream pull.")
        }
      }

      override def onDownstreamFinish(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"GCMDispatcherPrepare:: onDownstreamFinish")
        super.onDownstreamFinish()
      }

    })
    
    

  }

}
