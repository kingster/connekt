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
        ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: HttpResponse: $messageId")
        val stringResponse = r.entity.getString
        r.status.intValue() match {
          case 200 =>
            try {
              ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: HttpResponseBody: $stringResponse")

              val responseBody = stringResponse.getObj[ObjectNode]
              val deviceIdItr = deviceIds.listIterator()

              responseBody.findValue("results").foreach(rBlock => {
                val rDeviceId = deviceIdItr.next()
                rBlock match {
                  case s if s.has("message_id") =>
                    if (s.has("registration_id"))
                      DeviceDetailsService.get(appName, rDeviceId).foreach(_.foreach(d => {
                        ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Device token update notified via. $messageId for $rDeviceId")
                        DeviceDetailsService.update(d.deviceId, d.copy(token = s.get("registration_id").asText.trim))
                      }))

                    events += PNCallbackEvent(messageId, rDeviceId, GCMResponseStatus.Received, MobilePlatform.ANDROID, appName, "", s.get("message_id").asText(), eventTS)
                  case f if f.has("error") && List("InvalidRegistration", "NotRegistered").contains(f.get("error").asText.trim) =>
                    DeviceDetailsService.get(appName, rDeviceId).foreach {
                      _.foreach(device => if (device.osName == MobilePlatform.ANDROID.toString) {
                        ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Device token invalid / not_found. Deleting device details for $rDeviceId.")
                        DeviceDetailsService.delete(appName, device.deviceId)
                      })
                    }
                    events += PNCallbackEvent(messageId, rDeviceId, GCMResponseStatus.Error, MobilePlatform.ANDROID, appName, "", f.get("error").asText, eventTS)
                  case e: JsonNode =>
                    ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: Unknown Error [${e.toString}}] via. $messageId for $rDeviceId")
                    events += PNCallbackEvent(messageId, rDeviceId, GCMResponseStatus.Error, MobilePlatform.ANDROID, appName, "", e.toString, eventTS)
                }
              })
            } catch {
              case e: Exception =>
                events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.ParseError, MobilePlatform.ANDROID, appName, "", e.getMessage, eventTS)))
                ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: Failed Processing HttpResponseBody for: $messageId:: ${e.getMessage}", e)
            }

          case 400 =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.InvalidJsonError, MobilePlatform.ANDROID, appName, "", "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: HttpResponse - Invalid json sent for $messageId")
          case 401 =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.AuthError, MobilePlatform.ANDROID, appName, "", "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: HttpResponse - The sender account used to send a message couldn't be authenticated. for $messageId")
          case w if 5 == (w / 100) =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.InternalError, MobilePlatform.ANDROID, appName, "", "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: HttpResponse - The gcm server encountered an error while trying to process the request for $messageId")
          case _ =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.Error, MobilePlatform.ANDROID, appName, "", "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: HttpResponse - GCM Response Unhandled for $messageId")
        }

      case Failure(e2) =>
        events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, GCMResponseStatus.SendError, MobilePlatform.ANDROID, appName, "", e2.getMessage, eventTS)))
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: GCM send failure for r: $messageId, e: ${e2.getMessage}", e2)
    }

    events.persist
    events.toList
  }


  object GCMResponseStatus extends Enumeration {
    type GCMResponseStatus = Value

    val InvalidJsonError = Value("gcm_invalid_json_error")
    val AuthError = Value("gcm_auth_error")
    val Received = Value("gcm_received")
    val Error = Value("gcm_error")
    val InternalError = Value("gcm_internal_error")
    val SendError = Value("connekt_gcm_send_error")
    val ParseError = Value("connekt_gcm_response_parse_error")
  }

}
