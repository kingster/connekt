package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URL

import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.models.GCMRequestTrace
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.GCMPayloadEnvelope
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 * Created by kinshuk.bairagi on 02/02/16.
 */
class GCMDispatcher(uri: URL = new URL("https", "android.googleapis.com", 443, "/gcm/send"))
  extends GraphStage[FlowShape[GCMPayloadEnvelope, (HttpRequest, GCMRequestTrace)]] {

  val in = Inlet[GCMPayloadEnvelope]("GCMDispatcher.In")
  val out = Outlet[(HttpRequest, GCMRequestTrace)]("GCMDispatcher.Out")

  override def shape: FlowShape[GCMPayloadEnvelope, (HttpRequest, GCMRequestTrace)] = FlowShape.of(in, out)

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = BusyBeesBoot.mat

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {
        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"HttpDispatcher:: onPush:: Received Message: ${message.toString}")

        val requestEntity = HttpEntity(ContentTypes.`application/json`, message.gcmPayload.getJson)
        val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(message.appName).get.apiKey))
        val httpRequest = new HttpRequest(HttpMethods.POST, uri.getPath, requestHeaders, requestEntity)
        val requestTrace = GCMRequestTrace(message.messageId, message.deviceId, message.appName)

        ConnektLogger(LogFile.PROCESSORS).debug(s"HttpDispatcher:: onPush:: Request Payload : ${httpRequest.entity.asInstanceOf[Strict].data.decodeString("UTF-8")}")
        ConnektLogger(LogFile.PROCESSORS).debug(s"HttpDispatcher:: onPush:: Relayed Try[HttpResponse] to next stage for: ${requestTrace.messageId}")

        push(out, (httpRequest, requestTrace))
        if(isAvailable(out) && !hasBeenPulled(in))
          pull(in)
      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"HttpDispatcher:: onPush :: ${e.getMessage}", e)
          if(!hasBeenPulled(in))
            pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPull")
        pull(in)
      }
    })

  }

}