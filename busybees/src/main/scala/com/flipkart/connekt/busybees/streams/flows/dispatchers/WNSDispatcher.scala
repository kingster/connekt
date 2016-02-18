package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URI

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.WNSPNPayload
import com.flipkart.connekt.commons.services.WindowsTokenService
import com.flipkart.connekt.commons.utils.StringUtils

/**
 * @author aman.shrivastava on 08/02/16.
 */
class WNSDispatcher extends GraphStage[FlowShape[WNSPNPayload, (HttpRequest, (String, String))]] {
  val in = Inlet[WNSPNPayload]("WNSDispatcher.In")
  val out = Outlet[(HttpRequest, (String, String))]("WNSDispatcher.Out")

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPush:: Received Message: $message")
        val uri = new URI(message.token).toURL

        val headers = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "Bearer " + WindowsTokenService.getToken(message.appName).get.token), RawHeader("Content-Length", "500"), RawHeader("X-WNS-Type", message.wnsPNType.getWnsType))


        val payload = HttpEntity(message.wnsPNType.getContentType, message.wnsPNType.getPayload)


        val request = new HttpRequest(HttpMethods.POST, uri.getFile, headers, payload)
        val requestId = StringUtils.generateRandomStr(10)


      push(out, (request, (requestId, message.appName)))

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"WNSDispatcher:: onPush :: Error", e)
          pull(in)
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
        pull(in)
      }

      override def onDownstreamFinish(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info("WNSDispatcher:: onDownstreamFinish finish invoked")
        super.onDownstreamFinish()
      }
    })


    override def afterPostStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: postStop")

      //apnsClient.stop()
      super.afterPostStop()
    }

  }
}
