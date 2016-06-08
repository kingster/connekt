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
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.utils.HttpUtils._
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels.MessageStatus.{InternalStatus, WNSResponseStatus}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.services.{DeviceDetailsService, WindowsOAuthService}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class WNSResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseErrorHandler[(Try[HttpResponse], WNSRequestTracker), WNSRequestTracker] {

  val in = Inlet[(Try[HttpResponse], WNSRequestTracker)]("WNSResponseHandler.In")
  val out = Outlet[Try[Either[PNCallbackEvent, WNSResponseHandler]]]("WNSResponseHandler.Out")

  override val map: ((Try[HttpResponse], WNSRequestTracker)) => Future[List[Either[WNSRequestTracker, PNCallbackEvent]]] = responseTrackerPair => Future(profile("map") {
    handleWNSResponse(responseTrackerPair._1, responseTrackerPair._2) match {
      case Some(pnCallback) =>
        List(Right(pnCallback))
      case None =>
        List(Left(responseTrackerPair._2))
    }
  })

  private def handleWNSResponse(tryResponse: Try[HttpResponse], requestTracker: WNSRequestTracker): Option[PNCallbackEvent] = {

    val eventTS = System.currentTimeMillis()
    val requestId = requestTracker.requestId
    val appName = requestTracker.appName
    val deviceId = requestTracker.deviceId
    val contextId = requestTracker.contextId
    val client = requestTracker.clientId

    val maybePNCallbackEvent: Option[PNCallbackEvent] = tryResponse match {
      case Success(r) =>
        val response = r.entity.getString
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler received http response for r: $requestId")
        Option(r.status.intValue() match {
          case 200 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.Received)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler 200 for $requestId")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.Received, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 400 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.InvalidHeader)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler invalid/missing header send: $requestId response: $response")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.InvalidHeader, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 401 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.AuthError)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the cloud service is not authorized to send a notification to this uri even though they are authenticated: $requestId response: $response")
            WindowsOAuthService.refreshToken(appName)
            null
          case 403 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.InvalidMethod)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler invalid method (get, create); only post (windows or windows phone) or delete (windows phone only) is allowed $requestId response: $response")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 404 =>
            DeviceDetailsService.get(appName, deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                ServiceFactory.getReportingService.recordPushStatsDelta(client  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidChannelUri)
                DeviceDetailsService.delete(appName, deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device not found. deleting details of device: $deviceId wrt. message: $requestId response: $response")
                Success(PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.InvalidChannelUri, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
              case Some(dd)  =>
                ServiceFactory.getReportingService.recordPushStatsDelta( client  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device $deviceId platform does not match with connekt request platform for: $requestId response: $response")
                Success(PNCallbackEvent(requestId, client,deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId,r.optHeader("X-WNS-MSG-ID"), eventTS))
              case None =>
                ServiceFactory.getReportingService.recordPushStatsDelta( client  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device $deviceId doesn't exist: $requestId response: $response")
                Success(PNCallbackEvent(requestId, client, deviceId,WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
            }, Failure(_)).get
          case 405 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.InvalidMethod)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.$requestId response: $response")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 406 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.ThrottleLimitExceeded)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the cloud service exceeded its throttle limit. response: $response")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.ThrottleLimitExceeded, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 410 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the channel expired. response: $response")
            DeviceDetailsService.get(appName, deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                DeviceDetailsService.delete(appName, deviceId)
                ServiceFactory.getReportingService.recordPushStatsDelta( client  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName) , MobilePlatform.WINDOWS , WNSResponseStatus.ChannelExpired)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the channel expired. deleting device: $deviceId wrt message: $requestId")
                Success(PNCallbackEvent(requestId,client, deviceId, WNSResponseStatus.ChannelExpired, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
              case Some(dd)  =>
                ServiceFactory.getReportingService.recordPushStatsDelta( client  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device $deviceId platform does not match with connekt request platform $requestId")
                Success(PNCallbackEvent(requestId,client, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId,null, eventTS))
              case None =>
                ServiceFactory.getReportingService.recordPushStatsDelta( client  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device $deviceId doesn't exist for message: $requestId")
                Success(PNCallbackEvent(requestId,client, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
            }, Failure(_)).get
          case 412 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.PreConditionFailed)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler precondition failed: $requestId")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.PreConditionFailed, MobilePlatform.WINDOWS, appName, contextId, null, eventTS)
          case 413 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.EntityTooLarge)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the notification payload exceeds the 5000 byte size limit for: $requestId response: $response")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.EntityTooLarge, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case w if 5 == (w / 100) =>
            ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, WNSResponseStatus.InternalError)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler The wns server encountered an error while trying to process the request: $requestId code: $w response: $response")
            PNCallbackEvent(requestId, client, deviceId, WNSResponseStatus.InternalError, MobilePlatform.WINDOWS, appName, contextId, null, eventTS)
        })
      case Failure(e) =>
        ServiceFactory.getReportingService.recordPushStatsDelta(client, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(appName), MobilePlatform.WINDOWS, InternalStatus.ProviderSendError)
        ConnektLogger(LogFile.PROCESSORS).error(s"WNSResponseHandler failure: $requestId error: ${e.getMessage}", e)
        Some(PNCallbackEvent(requestId, client, deviceId, InternalStatus.ProviderSendError, MobilePlatform.WINDOWS, appName, contextId, e.getMessage, eventTS))
    }

    maybePNCallbackEvent.toList.persist
    maybePNCallbackEvent
  }
}
