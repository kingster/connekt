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
class WNSDispatcher extends GraphStage[FlowShape[WNSPNPayload, (HttpRequest, String, String)]] {
  val in = Inlet[WNSPNPayload]("APNSDispatcher.In")
  val out = Outlet[(HttpRequest, String, String)]("APNSDispatcher.Out")

  override def shape = FlowShape.of(in, out)

  var callback: AsyncCallback[String] = null

  //TODO : Change this to dynamic path.
  val logging = true

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPush:: Received Message: $message")

        ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPush:: Send Payload: " + message)
        val uri = new URI(message.token).toURL

        val headers = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "Bearer " + WindowsTokenService.getToken(message.appName)), RawHeader("Content-Length", "500"), RawHeader("X-WNS-Type", message.wnsPNType.getWnsType))


        val payload = HttpEntity(message.wnsPNType.getContentType, message.wnsPNType.getPayload)


        val request = new HttpRequest(HttpMethods.POST, uri.getFile, headers, payload)
        val requestId = StringUtils.generateRandomStr(10)


      push(out, (request, requestId, message.appName))

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"WNSDispatcher:: onPush :: Error", e)
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPull")
        pull(in)
      }
    })


    override def afterPostStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: postStop")

      //apnsClient.stop()
      super.afterPostStop()
    }

  }
}
