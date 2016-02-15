package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URI

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import ar.com.fernandospr.wns.WnsService
import ar.com.fernandospr.wns.client.WnsClient
import com.flipkart.connekt.commons.entities.WNSCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.WNSPNPayload
import com.flipkart.connekt.commons.utils.StringUtils

/**
 * @author aman.shrivastava on 08/02/16.
 */
class WNSDispatcher(credentials: WNSCredential) extends GraphStage[FlowShape[WNSPNPayload, (HttpRequest, String)]] {
  val in = Inlet[WNSPNPayload]("APNSDispatcher.In")
  val out = Outlet[(HttpRequest, String)]("APNSDispatcher.Out")

  override def shape = FlowShape.of(in, out)

  var callback: AsyncCallback[String] = null

  //TODO : Change this to dynamic path.
  val logging = true
  private lazy val wnsService = new WnsService(credentials.secureId, credentials.clientSecret, logging)
  private lazy val wnsClient = new WnsClient(credentials.secureId, credentials.clientSecret, logging)
  val token = "EgAbAQMAAAAEgAAAC4AAR37WcdhvnU89u0TKib5W8C4GKc5vwAVXuW0KvEp8yBUYy6OCNUrGIzlLrlh3SIy4050KPiYQdC4PWqvSHc5yZEQgz3vN066GvI0m7rSfBhtgrjGdjuFiVm0+2Yhrb364voV2PmP8LGMGt0hGglfL5nc2F357ihEky0xa/6mzH9mKAFoAigAAAAAAm8UXQEjMwFZIzMBW60gEAA8AMTA2LjUxLjEzNC4xMDgAAAAAAFsAbXMtYXBwOi8vcy0xLTE1LTItMjUyODk1ODI1NS0yMDI5MTk0NzQ2LTc0OTQxODgwNi02OTczNTU2NjgtMzQ4NDQ5NTIzNC0zMTUwMzEyOTctNDYxOTk4NjE1AA=="

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPush:: Received Message: $message")

        ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPush:: Send Payload: " + message)
        val uri = new URI(message.token).toURL

        val headers = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "Bearer " + token), RawHeader("Content-Length", "200"), RawHeader("X-WNS-Type", message.wnsPNType.getWnsType))


        val payload = HttpEntity(message.wnsPNType.getContentType, message.wnsPNType.getPayload)


        val request = new HttpRequest(HttpMethods.POST, uri.getFile, headers, payload)
        val requestId = StringUtils.generateRandomStr(10)


      push(out, (request, requestId))

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
