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
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.models.MessageStatus.{InternalStatus, WNSResponseStatus}
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.utils.HttpUtils._
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.services.{DeviceDetailsService, WindowsOAuthService}
import com.flipkart.connekt.commons.utils.StringUtils._

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
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler received: ${wnsResponse._2.requestId}")

        try {
          handleWNSResponse(wnsResponse._1, wnsResponse._2) match {
            case Some(pnCallbackEvent ) =>
              if(isAvailable(out)) {
                push[PNCallbackEvent](out, pnCallbackEvent)
                ConnektLogger(LogFile.PROCESSORS).trace(s"WNSResponseHandler pushed downstream for: ${wnsResponse._2.requestId}")
              }
            case None =>
              if(isAvailable(error))
                push[WNSRequestTracker](error, wnsResponse._2)
          }

        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"WNSResponseHandler error", e)
            if(!hasBeenPulled(in)) {
              pull(in)
              ConnektLogger(LogFile.PROCESSORS).trace(s"WNSResponseHandler pulled upstream for: ${wnsResponse._2.requestId}")
            }

            List(PNCallbackEvent(wnsResponse._2.requestId, wnsResponse._2.request.deviceId , "WNS_FAILED", MobilePlatform.WINDOWS,wnsResponse._2.request.appName, "TODO/CONTEXT", e.getMessage)).persist
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).trace(s"WNSResponseHandler pulled upstream on downstream.out pull")
        }
      }
    })

    setHandler(error, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).trace(s"WNSResponseHandler pulled upstream on downstream.error pull")
        }
      }
    })
  }

  private def handleWNSResponse(tryResponse: Try[HttpResponse], requestTracker: WNSRequestTracker): Option[PNCallbackEvent] = {

    val eventTS = System.currentTimeMillis()
    val requestId = requestTracker.requestId
    val appName = requestTracker.appName
    val deviceId = requestTracker.request.deviceId
    val contextId = requestTracker.request.contextId

    val maybePNCallbackEvent: Option[PNCallbackEvent] = tryResponse match {
      case Success(r) =>
        val response = r.entity.getString
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler received http response for r: $requestId")
        Option(r.status.intValue() match {
          case 200 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler 200 for $requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.Received, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 400 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler invalid/missing header send: $requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidHeader, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 401 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the cloud service is not authorized to send a notification to this uri even though they are authenticated: $requestId response: $response")
            WindowsOAuthService.refreshToken(appName)
            null
          case 403 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler invalid method (get, create); only post (windows or windows phone) or delete (windows phone only) is allowed $requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 404 =>
            DeviceDetailsService.get(appName, requestTracker.request.deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                DeviceDetailsService.delete(appName, requestTracker.request.deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device not found. deleting details of device: ${requestTracker.request.deviceId} wrt. message: $requestId response: $response")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidChannelUri, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
              case Some(dd)  =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} platform does not match with connekt request platform for: $requestId response: $response")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
              case None =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} doesn't exist: $requestId response: $response")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
            }, Failure(_)).get
          case 405 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.$requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 406 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the cloud service exceeded its throttle limit. response: $response")
            PNCallbackEvent(requestId, deviceId = deviceId, WNSResponseStatus.ThrottleLimitExceeded, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 410 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the channel expired. response: $response")
            DeviceDetailsService.get(appName, requestTracker.request.deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                DeviceDetailsService.delete(appName, requestTracker.request.deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the channel expired. deleting device: ${requestTracker.request.deviceId} wrt message: $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.ChannelExpired, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
              case Some(dd)  =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} platform does not match with connekt request platform $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
              case None =>
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} doesn't exist for message: $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
            }, Failure(_)).get
          case 412 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler precondition failed: $requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.PreConditionFailed, MobilePlatform.WINDOWS, appName, contextId, null, eventTS)
          case 413 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the notification payload exceeds the 5000 byte size limit for: $requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.EntityTooLarge, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case w if 5 == (w / 100) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler The wns server encountered an error while trying to process the request: $requestId code: $w response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InternalError, MobilePlatform.WINDOWS, appName, contextId, null, eventTS)
        })
      case Failure(e) =>
        Some(PNCallbackEvent(requestId, deviceId, InternalStatus.ProviderSendError, MobilePlatform.WINDOWS, appName, "", e.getMessage, eventTS))
    }

    maybePNCallbackEvent.toList.persist
    maybePNCallbackEvent
  }
}
