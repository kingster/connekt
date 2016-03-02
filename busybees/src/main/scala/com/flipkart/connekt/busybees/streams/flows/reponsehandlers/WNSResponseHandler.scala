package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.models.{GCMRequestTracker, WNSRequestTracker}
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.services.{DeviceDetailsService, WindowsTokenService}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import com.flipkart.connekt.commons.utils.StringUtils._
/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class WNSResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseErrorHandler[(Try[HttpResponse], WNSRequestTracker), WNSRequestTracker] {

  val in = Inlet[(Try[HttpResponse], WNSRequestTracker)]("WNSResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("WNSResponseHandler.Out")
  val error = Outlet[WNSRequestTracker]("WNSResponseHandler.Error")

  override def shape = new FanOutShape2(in, out, error)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = try {
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: OnPush")
        val wnsResponse = grab(in)
        handleWNSResponse(wnsResponse._1, wnsResponse._2) match {
          case Some(pnCallbackEvent ) =>
            if(isAvailable(out))
              push[PNCallbackEvent](out, pnCallbackEvent)
          case None =>
            if(isAvailable(error))
              push[WNSRequestTracker](error, wnsResponse._2)
        }

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"WNSResponseHandler:: onPush :: Error", e)
          if(!hasBeenPulled(in))
            pull(in)
      }
    })

    Seq(out, error).foreach(o => {
      setHandler(o, new OutHandler {
        override def onPull(): Unit = {
          if (!hasBeenPulled(in))
            pull(in)
        }
      })
    })
  }

  private def handleWNSResponse(tryResponse: Try[HttpResponse], requestTracker: WNSRequestTracker): Option[PNCallbackEvent] = {

    val eventTS = System.currentTimeMillis()
    val requestId = requestTracker.requestId
    val appName = requestTracker.appName
    val deviceId = requestTracker.request.deviceId

    val maybePNCallbackEvent: Option[PNCallbackEvent] = tryResponse match {
      case Success(r) =>
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Received httpResponse for r: $requestId")
        Option(r.status.intValue() match {
          case 200 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: 200")
            PNCallbackEvent(requestId, deviceId, MobilePlatform.WINDOWS, WNSResponseStatus.Received, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 400 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid/Missing header send:: $requestId")
            PNCallbackEvent(requestId, deviceId, MobilePlatform.WINDOWS, WNSResponseStatus.InvalidHeader, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 401 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The cloud service is not authorized to send a notification to this URI even though they are authenticated. $requestId")
            WindowsTokenService.refreshToken(appName)
            null
          case 403 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed. $requestId")
            PNCallbackEvent(requestId, deviceId, MobilePlatform.WINDOWS, WNSResponseStatus.InvalidMethod, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 404 =>
            DeviceDetailsService.get(appName, requestTracker.request.deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                DeviceDetailsService.delete(appName, requestTracker.request.deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The channel URI is not valid or is not recognized by WNS. Deleting Device [${requestTracker.request.deviceId}}] $requestId")
                Success(PNCallbackEvent(requestId, deviceId, MobilePlatform.WINDOWS, WNSResponseStatus.InvalidChannelUri, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
              case Some(dd)  =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Device [${requestTracker.request.deviceId}}] platform does not match with connekt Request platform $requestId")
                Success(PNCallbackEvent(requestId, deviceId, MobilePlatform.WINDOWS, WNSResponseStatus.InvalidDevice, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
              case None =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Device [${requestTracker.request.deviceId}}] doesn't exist $requestId")
                Success(PNCallbackEvent(requestId, deviceId, MobilePlatform.WINDOWS, WNSResponseStatus.DeletedDevice, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS))
            }, Failure(_)).get

          case 405 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.$requestId")
            PNCallbackEvent(requestId, deviceId, MobilePlatform.WINDOWS, WNSResponseStatus.InvalidMethod, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 406 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The cloud service exceeded its throttle limit.")
            PNCallbackEvent(requestId, deviceId = "", MobilePlatform.WINDOWS, WNSResponseStatus.ThrottleLimitExceeded, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 410 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The channel expired.")
            DeviceDetailsService.delete(requestTracker.appName, requestTracker.request.deviceId)
            PNCallbackEvent(requestId, deviceId = "", MobilePlatform.WINDOWS, WNSResponseStatus.ChannelExpired, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case 413 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The notification payload exceeds the 5000 byte size limit. $requestId")
            PNCallbackEvent(requestId, deviceId = "", MobilePlatform.WINDOWS, WNSResponseStatus.EntityTooLarge, appName, "", r.getHeader("X-WNS-MSG-ID").get.value(), eventTS)
          case w if 5 == (w / 100) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler:: The wns server encountered an error while trying to process the request. $requestId")
            PNCallbackEvent(requestId, deviceId = "", MobilePlatform.WINDOWS, WNSResponseStatus.InternalError, appName, "", "", eventTS)
        })
      case Failure(e) =>
        Some(PNCallbackEvent(requestId, deviceId = "", MobilePlatform.WINDOWS, WNSResponseStatus.RequestError, appName, "", e.getMessage, eventTS))
    }
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
