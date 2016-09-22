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

import akka.stream.Materializer
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.xmpp.XmppNackException
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ServiceFactory, LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.{XmppDownstreamResponse, PNCallbackEvent}
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels.MessageStatus.GCMResponseStatus
import com.flipkart.connekt.commons.metrics.Instrumented

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success, Try}

class XmppDownstreamHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[XmppDownstreamResponse], GCMRequestTracker)](96) with Instrumented {

  val badRegistrationError = "BAD_REGISTRATION"
  val invalidJson = "INVALID_JSON"
  val rateExceededError = "DEVICE_MESSAGE_RATE_EXCEEDED"

  override val map: ((Try[XmppDownstreamResponse], GCMRequestTracker)) => Future[List[PNCallbackEvent]] = responseTrackerPair => Future(profile("map") {

    val (xmppResponse,requestTracker) = responseTrackerPair

    val messageId = requestTracker.messageId
    val appName = requestTracker.appName
    val deviceId = requestTracker.deviceId.head

    val eventTS = System.currentTimeMillis()

    val (responseStatus, responseMessage) = xmppResponse match {
      case Success(response) =>
        ConnektLogger(LogFile.PROCESSORS).info(s"XmppDownstreamHandler received xmpp response for: $messageId")
        GCMResponseStatus.Received -> null

      case Failure(e: XmppNackException) =>
        val status = if (e.response.error.equalsIgnoreCase(badRegistrationError)) {
          DeviceDetailsService.get(appName, deviceId).foreach {
            _.foreach(device => if (device.osName == MobilePlatform.ANDROID.toString) {
              ConnektLogger(LogFile.PROCESSORS).info(s"XmppDownstreamHandler token invalid  deleting details of device: ${requestTracker.deviceId}.")
              DeviceDetailsService.delete(appName, deviceId)
            })
          }
          GCMResponseStatus.InvalidDevice
          } else if (e.response.error.equalsIgnoreCase(invalidJson))
            GCMResponseStatus.InvalidJsonError
          else
            GCMResponseStatus.Error
        ConnektLogger(LogFile.PROCESSORS).error(s"XmppDownstreamHandler: failed message: $messageId, reason: ${e.response.errorDescription}")
        status -> e.response.errorDescription

      case Failure(e: Exception) =>
        ConnektLogger(LogFile.PROCESSORS).error(s"XmppDownstreamHandler: failed message: $messageId, reason: ${e.getMessage}")
        GCMResponseStatus.Error -> e.getMessage
    }
    ServiceFactory.getReportingService.recordPushStatsDelta(requestTracker.clientId,
      Option(requestTracker.contextId),
      requestTracker.meta.get("stencilId").map(_.toString),
      Option(MobilePlatform.ANDROID.toString),
      requestTracker.appName,
      responseStatus)
    val events = List(PNCallbackEvent(messageId, requestTracker.clientId, deviceId, responseStatus, MobilePlatform.ANDROID, appName, requestTracker.contextId, responseMessage, eventTS))
    events.persist
    events
  })(m.executionContext)
}
