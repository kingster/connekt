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
import com.flipkart.connekt.busybees.models.{Buffer, WNSRequestTracker}
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


class WNSResponseHandler(parallelism:Int = 96)(implicit m: Materializer, ec: ExecutionContext)  extends PNProviderResponseErrorHandler[(Try[HttpResponse], WNSRequestTracker), WNSRequestTracker] {

  val in = Inlet[(Try[HttpResponse], WNSRequestTracker)]("WNSResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("WNSResponseHandler.Out")
  val error = Outlet[WNSRequestTracker]("WNSResponseHandler.Error")

  override def shape = new FanOutShape2(in, out, error)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    var buffer1: Buffer[PNCallbackEvent] = _
    var buffer2: Buffer[WNSRequestTracker] = _


    override def preStart() = {
      buffer1 = Buffer(parallelism, materializer)
      buffer2 = Buffer(parallelism, materializer)
    }

    var inFlight = 0

    private val futureCB =
      getAsyncCallback((result: Try[Any]) ⇒ {
        inFlight -= 1
        result match {
          case Success(elem) ⇒
            elem match {
              case e: PNCallbackEvent =>
                if (isAvailable(out)) {
                  if (!hasBeenPulled(in)) tryPull(in)
                  push(out, e)
                } else buffer1.enqueue(e)
              case e: WNSRequestTracker =>
                if (isAvailable(error)) {
                  if (!hasBeenPulled(in)) tryPull(in)
                  push(error, e)
                } else buffer2.enqueue(e)
            }
          case Failure(e) ⇒
            if (isClosed(in)) completeStage()
            else if (!hasBeenPulled(in)) tryPull(in)
        }
      }).invoke _

    private[this] def todo = inFlight + buffer1.used + buffer2.used

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val wnsResponse = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler received: ${wnsResponse._2.requestId}")

        try {
          val response = Future(handleWNSResponse(wnsResponse._1, wnsResponse._2).getOrElse(wnsResponse._2))
          inFlight += 1
          response.onComplete(futureCB)
        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"WNSResponseHandler error", e)
            if (todo < parallelism) {
              tryPull(in)
              ConnektLogger(LogFile.PROCESSORS).trace(s"WNSResponseHandler pulled upstream for: ${wnsResponse._2.requestId}")
            }
            PNCallbackEvent(wnsResponse._2.requestId, wnsResponse._2.request.deviceId, InternalStatus.WnsResponseHandleError, MobilePlatform.WINDOWS, wnsResponse._2.request.appName, wnsResponse._2.request.contextId, e.getMessage).persist
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (buffer1.nonEmpty) {
          push(out, buffer1.dequeue())
        }
        else if (isClosed(in)) completeStage()
        if (todo < parallelism && !hasBeenPulled(in)) tryPull(in)
      }
    })

    setHandler(error, new OutHandler {
      override def onPull(): Unit = {
        if (buffer2.nonEmpty) {
          push(error, buffer2.dequeue())
        }
        else if (isClosed(in)) completeStage()
        if (todo < parallelism && !hasBeenPulled(in)) tryPull(in)
      }
    })
  }

  private def handleWNSResponse(tryResponse: Try[HttpResponse], requestTracker: WNSRequestTracker): Option[PNCallbackEvent] =  {

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
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.Received)
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.Received, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 400 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.InvalidHeader)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler invalid/missing header send: $requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidHeader, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 401 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.AuthError)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the cloud service is not authorized to send a notification to this uri even though they are authenticated: $requestId response: $response")
            WindowsOAuthService.refreshToken(appName)
            null
          case 403 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.InvalidMethod)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler invalid method (get, create); only post (windows or windows phone) or delete (windows phone only) is allowed $requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 404 =>
            DeviceDetailsService.get(appName, requestTracker.request.deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                ServiceFactory.getReportingService.recordPushStatsDelta( requestTracker.meta.get("client").getString  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidChannelUri)
                DeviceDetailsService.delete(appName, requestTracker.request.deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device not found. deleting details of device: ${requestTracker.request.deviceId} wrt. message: $requestId response: $response")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidChannelUri, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
              case Some(dd)  =>
                ServiceFactory.getReportingService.recordPushStatsDelta( requestTracker.meta.get("client").getString  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} platform does not match with connekt request platform for: $requestId response: $response")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
              case None =>
              ServiceFactory.getReportingService.recordPushStatsDelta( requestTracker.meta.get("client").getString  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} doesn't exist: $requestId response: $response")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS))
            }, Failure(_)).get
          case 405 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.InvalidMethod)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler Invalid method (GET, CREATE); only POST (Windows or Windows Phone) or DELETE (Windows Phone only) is allowed.$requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidMethod, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 406 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.ThrottleLimitExceeded)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the cloud service exceeded its throttle limit. response: $response")
            PNCallbackEvent(requestId, deviceId = deviceId, WNSResponseStatus.ThrottleLimitExceeded, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case 410 =>
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the channel expired. response: $response")
            DeviceDetailsService.get(appName, requestTracker.request.deviceId).transform[PNCallbackEvent]({
              case Some(dd) if dd.osName == "windows" =>
                DeviceDetailsService.delete(appName, requestTracker.request.deviceId)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the channel expired. deleting device: ${requestTracker.request.deviceId} wrt message: $requestId")
                ServiceFactory.getReportingService.recordPushStatsDelta( requestTracker.meta.get("client").getString  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName) , MobilePlatform.WINDOWS , WNSResponseStatus.ChannelExpired)
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.ChannelExpired, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
              case Some(dd)  =>
                ServiceFactory.getReportingService.recordPushStatsDelta( requestTracker.meta.get("client").getString  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} platform does not match with connekt request platform $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
              case None =>
              ServiceFactory.getReportingService.recordPushStatsDelta( requestTracker.meta.get("client").getString  ,Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName) , MobilePlatform.WINDOWS , WNSResponseStatus.InvalidDevice)
                ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler device ${requestTracker.request.deviceId} doesn't exist for message: $requestId")
                Success(PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InvalidDevice, MobilePlatform.WINDOWS, appName, contextId, null, eventTS))
            }, Failure(_)).get
          case 412 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.PreConditionFailed)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler precondition failed: $requestId")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.PreConditionFailed, MobilePlatform.WINDOWS, appName, contextId, null, eventTS)
          case 413 =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.EntityTooLarge)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler the notification payload exceeds the 5000 byte size limit for: $requestId response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.EntityTooLarge, MobilePlatform.WINDOWS, appName, contextId, r.optHeader("X-WNS-MSG-ID"), eventTS)
          case w if 5 == (w / 100) =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, WNSResponseStatus.InternalError)
            ConnektLogger(LogFile.PROCESSORS).info(s"WNSResponseHandler The wns server encountered an error while trying to process the request: $requestId code: $w response: $response")
            PNCallbackEvent(requestId, deviceId, WNSResponseStatus.InternalError, MobilePlatform.WINDOWS, appName, contextId, null, eventTS)
        })
      case Failure(e) =>
        ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(requestTracker.appName), MobilePlatform.WINDOWS, InternalStatus.ProviderSendError)
        ConnektLogger(LogFile.PROCESSORS).error(s"WNSResponseHandler failure: $requestId error: ${e.getMessage}", e)
        Some(PNCallbackEvent(requestId, deviceId, InternalStatus.ProviderSendError, MobilePlatform.WINDOWS, appName, contextId, e.getMessage, eventTS))
    }

    maybePNCallbackEvent.toList.persist
    maybePNCallbackEvent
  }
}
