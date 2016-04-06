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
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.models.MessageStatus.{InternalStatus, GCMResponseStatus}
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class GCMResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[HttpResponse], GCMRequestTracker)] {

  override val map: ((Try[HttpResponse], GCMRequestTracker)) => List[PNCallbackEvent] = responseTrackerPair => {

    val tryResponse = responseTrackerPair._1
    val requestTracker = responseTrackerPair._2

    val messageId = requestTracker.messageId
    val appName = requestTracker.appName
    val deviceIds = requestTracker.deviceId

    val events = ListBuffer[PNCallbackEvent]()
    val eventTS = System.currentTimeMillis()

    tryResponse match {
      case Success(r) =>
        val stringResponse = r.entity.getString
        ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler received http response for: $messageId")
        ConnektLogger(LogFile.PROCESSORS).trace(s"GCMResponseHandler received http response for: $messageId http response body: $stringResponse")
        r.status.intValue() match {
          case 200 =>
            try {
              val responseBody = stringResponse.getObj[ObjectNode]
              val deviceIdItr = deviceIds.iterator

              responseBody.findValue("results").foreach(rBlock => {
                val rDeviceId = deviceIdItr.next()
                rBlock match {
                  case s if s.has("message_id") =>
                    if (s.has("registration_id"))
                      DeviceDetailsService.get(appName, rDeviceId).foreach(_.foreach(d => {
                        ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler device token update notified on. $messageId of device: $rDeviceId")
                        DeviceDetailsService.update(d.deviceId, d.copy(token = s.get("registration_id").asText.trim))
                      }))

                    events += PNCallbackEvent(messageId, rDeviceId, GCMResponseStatus.Received, MobilePlatform.ANDROID, appName, requestTracker.contextId, s.get("message_id").asText(), eventTS)
                  case f if f.has("error") && List("InvalidRegistration", "NotRegistered").contains(f.get("error").asText.trim) =>
                    DeviceDetailsService.get(appName, rDeviceId).foreach {
                      _.foreach(device => if (device.osName == MobilePlatform.ANDROID.toString) {
                        ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler device token invalid / not_found, deleting details of device: $rDeviceId.")
                        DeviceDetailsService.delete(appName, device.deviceId)
                      })
                    }
                    events += PNCallbackEvent(messageId, rDeviceId, GCMResponseStatus.Error, MobilePlatform.ANDROID, appName, requestTracker.contextId, f.get("error").asText, eventTS)
                  case e: JsonNode =>
                    ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler unknown for message: $messageId, device: $rDeviceId", e)
                    events += PNCallbackEvent(messageId, rDeviceId, GCMResponseStatus.Error, MobilePlatform.ANDROID, appName, requestTracker.contextId, e.toString, eventTS)
                }
              })
            } catch {
              case e: Exception =>
                events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, InternalStatus.ParseError, MobilePlatform.ANDROID, appName, requestTracker.contextId, e.getMessage, eventTS)))
                ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler failed processing http response body for: $messageId", e)
            }

          case 400 =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.InvalidJsonError, MobilePlatform.ANDROID, appName, requestTracker.contextId, "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler http response - invalid json sent for: $messageId")
          case 401 =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.AuthError, MobilePlatform.ANDROID, appName, requestTracker.contextId, "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler http response - the sender account used to send a message couldn't be authenticated for: $messageId")
          case w if 5 == (w / 100) =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.InternalError, MobilePlatform.ANDROID, appName, requestTracker.contextId, "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler http response - the gcm server encountered an error while trying to process the request for: $messageId")
          case _ =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.Error, MobilePlatform.ANDROID, appName, requestTracker.contextId, "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler http response - gcm response unhandled for: $messageId")
        }

      case Failure(e2) =>
        events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, InternalStatus.ProviderSendError, MobilePlatform.ANDROID, appName, requestTracker.contextId, e2.getMessage, eventTS)))
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler gcm send failure for: $messageId", e2)
    }

    events.persist
    events.toList
  }
}
