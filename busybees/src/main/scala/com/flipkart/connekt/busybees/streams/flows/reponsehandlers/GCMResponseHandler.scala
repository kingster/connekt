package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.commons.entities.{MobilePlatform, Channel}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class GCMResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[HttpResponse], GCMRequestTracker)] {

  val in = Inlet[(Try[HttpResponse], GCMRequestTracker)]("GCMResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("GCMResponseHandler.Out")

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: onPush.")
        val gcmResponse = grab(in)
        val outEvents = handleGCMResponse(gcmResponse._1, gcmResponse._2)

        if (isAvailable(out) && outEvents.nonEmpty)
          emitMultiple[PNCallbackEvent](out,immutable.Iterable.concat(outEvents))
        else if (!hasBeenPulled(in))
          pull(in)


      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: onPush Error: ${e.getMessage}", e)
          if (!hasBeenPulled(in))
            pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in))
          pull(in)
      }
    })
  }

  private def handleGCMResponse(tryResponse: Try[HttpResponse], requestTracker: GCMRequestTracker): List[PNCallbackEvent] = {
    val messageId = requestTracker.messageId
    val appName = requestTracker.appName
    val deviceIds = requestTracker.deviceId

    val events = ListBuffer[PNCallbackEvent]()
    val eventTS = System.currentTimeMillis()

    tryResponse match {
      case Success(r) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: HttpResponse: $messageId")
        r.status.intValue() match {
          case 200 =>
            try {
              val stringResponse = r.entity.getString
              ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: HttpResponseBody: $stringResponse")

              val responseBody = stringResponse.getObj[ObjectNode]
              val deviceIdItr = deviceIds.listIterator()

              responseBody.findValue("results").foreach(rBlock => {
                val rDeviceId = deviceIdItr.next()
                rBlock match {
                  case s if s.has("message_id") =>
                    if (s.has("registration_id"))
                      DeviceDetailsService.get(appName, rDeviceId).foreach(_.foreach(d => DeviceDetailsService.update(d.deviceId, d.copy(token = s.get("registration_id").asText.trim))))

                    events += PNCallbackEvent(messageId, rDeviceId, MobilePlatform.ANDROID, GCMResponseStatus.Received, appName, "", s.get("message_id").asText(), eventTS)
                  case f if f.has("error") && List("InvalidRegistration", "NotRegistered").contains(f.get("error").asText.trim) =>
                    DeviceDetailsService.get(appName, rDeviceId)
                      .foreach(_.foreach(device => if (device.osName == MobilePlatform.ANDROID.toString) {
                        DeviceDetailsService.delete(appName, device.deviceId)
                      }))
                    events += PNCallbackEvent(messageId, rDeviceId, MobilePlatform.ANDROID, GCMResponseStatus.Error, appName, "", f.get("error").asText, eventTS)
                }
              })
            } catch {
              case e: Exception =>
                events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, MobilePlatform.ANDROID, GCMResponseStatus.ParseError, appName, "", e.getMessage, eventTS)))
                ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: Failed Processing HttpResponseBody for: $messageId:: ${e.getMessage}", e)
            }

          case 400 =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, MobilePlatform.ANDROID, GCMResponseStatus.InvalidJsonError, appName, "", "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: HttpResponse - Invalid json sent for $messageId")
          case 401 =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, MobilePlatform.ANDROID, GCMResponseStatus.AuthError, appName, "", "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: HttpResponse - The sender account used to send a message couldn't be authenticated. for $messageId")
          case w if 5 == (w / 100) =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, MobilePlatform.ANDROID, GCMResponseStatus.InternalError, appName, "", "", eventTS)))
            ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: HttpResponse - The gcm server encountered an error while trying to process the request for $messageId")
        }

      case Failure(e2) =>
        events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, MobilePlatform.ANDROID, GCMResponseStatus.SendError, appName, "", e2.getMessage, eventTS)))
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: GCM send failure for r: $messageId, e: ${e2.getMessage}", e2)
    }

    events.foreach(e => ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, e.deviceId, Channel.PUSH, e))
    ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: Saved callback events for $messageId ${events.toList.toString()}")
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
