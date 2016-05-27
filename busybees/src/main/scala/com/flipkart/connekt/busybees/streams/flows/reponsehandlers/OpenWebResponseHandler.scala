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
import com.flipkart.connekt.busybees.models.OpenWebRequestTracker
import com.flipkart.connekt.busybees.utils.HttpUtils._
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels.MessageStatus.{InternalStatus, OpenWebResponseStatus}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class OpenWebResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[HttpResponse], OpenWebRequestTracker)](96) with Instrumented {

  override val map: ((Try[HttpResponse], OpenWebRequestTracker)) => Future[List[PNCallbackEvent]] = responseTrackerPair => Future({

    val httpResponse = responseTrackerPair._1
    val requestTracker = responseTrackerPair._2

    val messageId = requestTracker.messageId
    val appName = requestTracker.appName
    val deviceId = requestTracker.deviceId

    val events = ListBuffer[PNCallbackEvent]()
    val eventTS = System.currentTimeMillis()

    httpResponse match {
      case Success(r) =>
        try {
          val stringResponse = r.entity.getString(m)
          ConnektLogger(LogFile.PROCESSORS).info(s"OpenWebResponseHandler received http response for: $messageId")
          ConnektLogger(LogFile.PROCESSORS).trace(s"OpenWebResponseHandler received http response for: $messageId http [${r.status.intValue()}] response body: $stringResponse")
          r.status.intValue() match {
            case 201 =>
              val providerMessageId = r.optHeader("Location")
              ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(requestTracker.contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(MobilePlatform.OPENWEB.toString), requestTracker.appName, OpenWebResponseStatus.Received)
              events += PNCallbackEvent(messageId, deviceId, OpenWebResponseStatus.Received, MobilePlatform.OPENWEB, appName, requestTracker.contextId, providerMessageId, eventTS)
            case w if 4 == (w / 100) =>
              // TODO: not getting json response for chrome
              /**
               * Chrome is yet to return proper json
               * curl -vvv -X POST -H "Accept: application/json" -H "Authorization: key=AIzaSyBCf_cvl1hkMQJvZ8LFlDwo6BR3J_hSuEM" -H "Content-length: 0" -H "TTL: 21600" "https://gcm-http.googleapis.com/gcm/eHW3324234234_Dj74uEM:APA91bEoKPRfR4VWK8bs5p5LqERfL9iG9RAZeqQvzSoE1wlF3N0varb5v6x8ryenmYXFyD-HJQ6M4t7V8lb3oaRqnV12X-1nSA5K8Yh0jw2dnHRHWyKvdfCZVR8puK_YZ7WNQe9ZwmQL"
               *
               * Firefox returns proper json
               * curl -vvv -X POST -H "Accept: application/json" -H "Content-length: 0" -H "TTL: 21600" "https://updates.push.services.mozilla.com/push/v1/gAAAAABXOvMWgkCNeTlt45ka7aax6zP929q_pIdBLQG7FMhPfTOdxSGNcEALzcai1NszuF1hZoRYBbxWSMC0lFNs6Z2b2PNQVsssp_YZriAUcZJUZHluwrBPTtYwu41fXwzpbxSfPR8KKUv"
               *
               */
              if (stringResponse.contains("UnauthorizedRegistration")) {
                ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(requestTracker.contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(MobilePlatform.OPENWEB.toString), requestTracker.appName, OpenWebResponseStatus.InvalidToken)
                events += PNCallbackEvent(messageId, deviceId, OpenWebResponseStatus.InvalidToken, MobilePlatform.OPENWEB, appName, requestTracker.contextId, stringResponse, eventTS)
                DeviceDetailsService.get(appName, deviceId).get.foreach(device => if (device.osName == MobilePlatform.OPENWEB.toString) {
                  ConnektLogger(LogFile.PROCESSORS).info(s"OpenWebResponseHandler device token invalid / not_found, deleting details of device: $appName / $deviceId.")
                  DeviceDetailsService.delete(appName, deviceId)
                })
              } else {
                ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(requestTracker.contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(MobilePlatform.OPENWEB.toString), requestTracker.appName, OpenWebResponseStatus.InvalidPayload)
                events += PNCallbackEvent(messageId, deviceId, OpenWebResponseStatus.InvalidPayload, MobilePlatform.OPENWEB, appName, requestTracker.contextId, stringResponse, eventTS)
              }
              ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebResponseHandler http response - invalid json sent for: $messageId response: $stringResponse")
            case w if 5 == (w / 100) =>
              ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(requestTracker.contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(MobilePlatform.OPENWEB.toString), requestTracker.appName, OpenWebResponseStatus.InternalError)
              events += PNCallbackEvent(messageId, deviceId, OpenWebResponseStatus.InternalError, MobilePlatform.OPENWEB, appName, requestTracker.contextId, "", eventTS)
              ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebResponseHandler http response - the server encountered an error while trying to process the request for: $messageId code: $w response: $stringResponse")
            case w =>
              ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(requestTracker.contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(MobilePlatform.OPENWEB.toString), requestTracker.appName, OpenWebResponseStatus.Error)
              events += PNCallbackEvent(messageId, deviceId, OpenWebResponseStatus.Error, MobilePlatform.OPENWEB, appName, requestTracker.contextId, stringResponse, eventTS)
              ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebResponseHandler http response - response unhandled for: $messageId code: $w response: $stringResponse")
          }
        } catch {
          case e: Exception =>
            ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(requestTracker.contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(MobilePlatform.OPENWEB.toString), requestTracker.appName, InternalStatus.OpenWebResponseHandleError)
            events += PNCallbackEvent(messageId, deviceId, InternalStatus.OpenWebResponseHandleError, MobilePlatform.OPENWEB, appName, requestTracker.contextId, e.getMessage, eventTS)
            ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebResponseHandler failed processing http response body for: $messageId", e)
        }
      case Failure(e2) =>
        ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.meta.get("client").getString, Option(requestTracker.contextId), requestTracker.meta.get("stencilId").map(_.toString), Option(MobilePlatform.OPENWEB.toString), requestTracker.appName, InternalStatus.ProviderSendError)
        events += PNCallbackEvent(messageId, deviceId, InternalStatus.ProviderSendError, MobilePlatform.OPENWEB, appName, requestTracker.contextId, e2.getMessage, eventTS)
        ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebResponseHandler  send failure for: $messageId", e2)
    }

    events.persist
    events.toList
  })(m.executionContext)
}
