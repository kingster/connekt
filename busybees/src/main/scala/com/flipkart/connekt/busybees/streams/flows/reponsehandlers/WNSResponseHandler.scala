package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.services.WindowsTokenService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class WNSResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseErrorHandler[(Try[HttpResponse], WNSRequestTracker),WNSRequestTracker] {

  val in = Inlet[(Try[HttpResponse], WNSRequestTracker)]("WNSResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("WNSResponseHandler.Out")
  val error = Outlet[WNSRequestTracker]("WNSResponseHandler.Error")

  override def shape  = new FanOutShape2(in, out, error)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = try {

        val wnsResponse = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Input ${wnsResponse}")

        val eventTS = System.currentTimeMillis()

        val requestId = wnsResponse._2.requestId

        val maybePNCallbackEvent: Option[PNCallbackEvent] =  wnsResponse._1 match {
          case Success(r) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Received httpResponse for r: ${wnsResponse._2}")
            Option(r.status.intValue() match {
              case 200 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Response:: $wnsResponse")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "WNS_RECEIVED", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case 400 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid/Missing header send:: $wnsResponse")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "INVALID_HEADER", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case 401 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The cloud service is not authorized to send a notification to this URI even though they are authenticated.")
                WindowsTokenService.refreshToken(wnsResponse._2.appName)
                null
              case 403 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "INVALID_METHOD", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case 404 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The channel URI is not valid or is not recognized by WNS.")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "INVALID_CHANNEL_URI", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case 405 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "INVALID_METHOD", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case 406 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The cloud service exceeded its throttle limit.")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "THROTTLE_LIMIT_EXCEEDED", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case 410 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The channel expired.")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "CHANNEL_EXPIRED", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case 413 =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The notification payload exceeds the 5000 byte size limit.")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "ENTITY_TOO_LARGE", appName = wnsResponse._2.appName, contextId = "", cargo = r.getHeader("X-WNS-MSG-ID").get.value(), timestamp = eventTS)
              case w if 5 == (w/100) =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The wns server encountered an error while trying to process the request.")
                PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "WNS_INTERNAL_ERROR", appName = wnsResponse._2.appName, contextId = "", cargo = null, timestamp = eventTS)
            })
          case Failure(e) =>
            Some(PNCallbackEvent(messageId = requestId, deviceId = "", platform = "windows", eventType = "WNS_REQUEST_ERROR", appName = wnsResponse._2.appName, contextId = "", cargo = e.getMessage, timestamp = eventTS))
        }

        maybePNCallbackEvent match {
          case Some(pnCallbackEvent ) =>
            if(isAvailable(out))
              push[PNCallbackEvent](out, pnCallbackEvent)
          case None =>
            if(isAvailable(error))
              push[WNSRequestTracker](error, wnsResponse._2)
        }

      }catch {
        case e:Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"WNSResponseHandler:: onPush :: Error", e)
          if(!hasBeenPulled(in))
            pull(in)
      }
    })

    Seq(out, error).foreach( o => {
      setHandler(o , new OutHandler {
        override def onPull(): Unit = {
          if (!hasBeenPulled(in))
            pull(in)
        }
      })
    })


  }
}
