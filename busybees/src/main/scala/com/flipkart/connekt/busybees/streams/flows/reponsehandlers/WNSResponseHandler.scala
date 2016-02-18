package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.services.WindowsTokenService

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class WNSResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[HttpResponse], String, String)] {

  val in = Inlet[(Try[HttpResponse], String, String)]("WNSResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("WNSResponseHandler.Out")

  override def shape  = FlowShape.of(in, out)


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val wnsResponse = grab(in)

        println("wnsResponse = " + wnsResponse)
        var event: PNCallbackEvent = null
        val eventTS = System.currentTimeMillis()

        wnsResponse._1 match {
          case Success(r) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Received httpResponse for r: ${wnsResponse._2}")
            r.status.intValue() match {
              case 200 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "WNS_RECEIVED", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Response:: $wnsResponse")
              case 400 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "INVALID_HEADER", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Invalid/Missing header send:: $wnsResponse")
              case 401 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "UNAUTHORIZED", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                WindowsTokenService.refreshToken(wnsResponse._3)

                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The cloud service is not authorized to send a notification to this URI even though they are authenticated.")
              case 403 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "INVALID_METHOD", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.")
              case 404 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "INVALID_CHANNEL_URI", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The channel URI is not valid or is not recognized by WNS.")
              case 405 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "INVALID_METHOD", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.")
              case 406 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "THROTTLE_LIMIT_EXCEEDED", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The cloud service exceeded its throttle limit.")
              case 410 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "CHANNEL_EXPIRED", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The channel expired.")
              case 413 =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "windows", eventType = "ENTITY_TOO_LARGE", appName = "", contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The notification payload exceeds the 5000 byte size limit.")
              case w if 5 == (w/100) =>
                event = PNCallbackEvent(messageId = wnsResponse._2, deviceId = "", platform = "android", eventType = "WNS_INTERNAL_ERROR", appName = "", contextId = "", cargo = wnsResponse._2, timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The wns server encountered an error while trying to process the request.")
              case _ =>
            }
        }


        push[PNCallbackEvent](out, event)
      }catch {
        case e:Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: onPush :: Error", e)
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }
    })

  }
}
