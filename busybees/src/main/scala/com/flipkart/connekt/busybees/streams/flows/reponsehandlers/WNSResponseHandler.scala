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
package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream._
import akka.stream.scaladsl.Sink
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.services.{DeviceDetailsService, WindowsOAuthService}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class WNSResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseErrorHandler[(Try[HttpResponse], WNSRequestTracker), WNSRequestTracker] {

  val in = Inlet[(Try[HttpResponse], WNSRequestTracker)]("WNSResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("WNSResponseHandler.Out")
  val error = Outlet[WNSRequestTracker]("WNSResponseHandler.Error")

  override def shape = new FanOutShape2(in, out, error)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val wnsResponse = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"WNSResponseHandler:: ON_PUSH for ${wnsResponse._2.requestId}")

        try {
          handleWNSResponse(wnsResponse._1, wnsResponse._2) match {
            case Some(pnCallbackEvent ) =>
              if(isAvailable(out)) {
                push[PNCallbackEvent](out, pnCallbackEvent)
                ConnektLogger(LogFile.PROCESSORS).debug(s"WNSResponseHandler:: PUSHED downstream for ${wnsResponse._2.requestId}")
              }
            case None =>
              if(isAvailable(error))
                push[WNSRequestTracker](error, wnsResponse._2)
          }

        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"WNSResponseHandler:: onPush :: Error", e)
            if(!hasBeenPulled(in)) {
              pull(in)
              ConnektLogger(LogFile.PROCESSORS).debug(s"WNSResponseHandler:: PULLED upstream for ${wnsResponse._2.requestId}")
            }

            List(PNCallbackEvent(wnsResponse._2.requestId, wnsResponse._2.request.deviceId , "WNS_FAILED", MobilePlatform.WINDOWS,wnsResponse._2.request.appName, "TODO/CONTEXT", e.getMessage)).persist
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).debug(s"WNSResponseHandler:: PULLED upstream on downstream.out pull.")
        }
      }
    })

    setHandler(error, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).debug(s"WNSResponseHandler:: PULLED upstream on downstream.error pull.")
        }
      }
    })
  }

  private def handleWNSResponse(tryResponse: Try[HttpResponse], requestTracker: WNSRequestTracker): Option[PNCallbackEvent] = {

    val eventTS = System.currentTimeMillis()
    val requestId = requestTracker.requestId
    val appName = requestTracker.appName
    val deviceId = requestTracker.request.deviceId

    val maybePNCallbackEvent: Option[PNCallbackEvent] = tryResponse match {
      case Success(r) =>
        r.entity.dataBytes.to(Sink.ignore)
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Received httpResponse for r: $requestId")
        Option(r.status.intValue() match {
          case 200 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: 200")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.Received, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 400 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid/Missing header send:: $requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidHeader, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 401 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The cloud service is not authorized to send a notification to this URI even though they are authenticated. $requestId")
            WindowsOAuthService.refreshToken(appName)
            null
          case 403 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed. $requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 404 =>
            DeviceDetailsService.get(appName, requestTracker.request.deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                DeviceDetailsService.delete(appName, requestTracker.request.deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The channel URI is not valid or is not recognized by WNS. Deleting Device [${requestTracker.request.deviceId}}] $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidChannelUri, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
              case Some(dd)  =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Device [${requestTracker.request.deviceId}}] platform does not match with connekt Request platform $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
              case None =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Device [${requestTracker.request.deviceId}}] doesn't exist $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.DeletedDevice, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
            }, Failure(_)).get

          case 405 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.$requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 406 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The cloud service exceeded its throttle limit.")
            PNCallbackEvent(requestId, deviceId = "", WNSResponseStatus.ThrottleLimitExceeded, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 410 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The channel expired.")
            DeviceDetailsService.get(appName, requestTracker.request.deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                DeviceDetailsService.delete(appName, requestTracker.request.deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The channel expired. Deleting Device [${requestTracker.request.deviceId}}] $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidChannelUri, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
              case Some(dd)  =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Device [${requestTracker.request.deviceId}}] platform does not match with connekt Request platform $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
              case None =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Device [${requestTracker.request.deviceId}}] doesn't exist $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.DeletedDevice, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
            }, Failure(_)).get
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.ChannelExpired, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 413 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The notification payload exceeds the 5000 byte size limit. $requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.EntityTooLarge, MobilePlatform.WINDOWS, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case w if 5 == (w / 100) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The wns server encountered an error while trying to process the request. $requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InternalError, MobilePlatform.WINDOWS, appName, "", "", eventTS)
        })
      case Failure(e) =>
        Some(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.RequestError, MobilePlatform.WINDOWS, appName, "", e.getMessage, eventTS))
    }

    maybePNCallbackEvent.toList.persist
    maybePNCallbackEvent
  }

  object WNSResponseStatus extends Enumeration {
    type WNSResponseStatus = Value
    
    val Received = Value("wns_received")
    val InvalidHeader = Value("wns_invalid_header")
    val InvalidMethod = Value("wns_invalid_method")
    val InvalidChannelUri = Value("wns_invalid_channel_uri")
    val InvalidDevice = Value("wns_invalid_device")
    val DeletedDevice = Value("wns_deleted_device")
    val ThrottleLimitExceeded = Value("wns_throttle_limit_exceeded")
    val ChannelExpired = Value("wns_channel_expired")
    val EntityTooLarge = Value("wns_entity_too_large")
    val InternalError = Value("wns_internal_error")
    val RequestError = Value("connekt_wns_send_error")
  }
}
