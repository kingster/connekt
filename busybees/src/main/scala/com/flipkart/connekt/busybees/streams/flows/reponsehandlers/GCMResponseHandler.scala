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

        val gcmResponse = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: onPush.")

        val messageId = gcmResponse._2.messageId
        val appName = gcmResponse._2.appName
        val deviceIds = gcmResponse._2.deviceId

        val events = ListBuffer[PNCallbackEvent]()
        val eventTS = System.currentTimeMillis()

        gcmResponse._1 match {
          case Success(r) =>
            ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: HttpResponse: $messageId")
            r.status.intValue() match {
              case 200 =>
                try {
                  val responseBuilder = Await.result(r.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)

                  ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: HttpResponseBody: $responseBuilder")
                  val responseBody = responseBuilder.getObj[ObjectNode]
                  val deviceIdItr = deviceIds.listIterator()

                  responseBody.findValue("results").foreach(rBlock => {
                    val rDeviceId = deviceIdItr.next()
                    rBlock match {
                      case s if s.has("message_id") =>
                        if (s.has("registration_id"))
                          DeviceDetailsService.get(appName, rDeviceId).foreach(_.foreach(d => DeviceDetailsService.update(d.deviceId, d.copy(token = s.get("registration_id").asText.trim))))
                        events += PNCallbackEvent(messageId, deviceId = deviceIdItr.next(), platform = "android", eventType = "GCM_RECEIVED", appName = appName, contextId = "", cargo = s.get("message_id").asText(), timestamp = eventTS)
                      case f if f.has("error") && List("InvalidRegistration", "NotRegistered").contains(f.get("error").asText.trim) =>
                        DeviceDetailsService.get(appName, rDeviceId)
                          .foreach(_.foreach(device => if (device.osName == MobilePlatform.ANDROID.toString) {
                            DeviceDetailsService.delete(appName, device.deviceId)
                          }))
                        events += PNCallbackEvent(messageId, rDeviceId, platform = "android", eventType = "GCM_ERROR", appName = appName, contextId = "", cargo = f.get("error").asText, timestamp = eventTS)
                    }
                  })
                } catch {
                  case e: Exception =>
                    ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: Failed Processing HttpResponseBody for: $messageId:: ${e.getMessage}", e)
                    events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, "android", "GCM_RESPONSE_PARSE_ERROR", appName, "", e.getMessage, eventTS)))
                }

              case 400 =>
                events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, "android", "GCM_INVALID_JSON", appName, "", "", eventTS)))
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: HttpResponse - Invalid json sent for $messageId")
              case 401 =>
                events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, "android", "GCM_AUTH_ERROR", appName, "", "", eventTS)))
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: HttpResponse - The sender account used to send a message couldn't be authenticated. for $messageId")
              case w if 5 == (w / 100) =>
                events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, "android", "GCM_INTERNAL_ERROR", appName, "", "", eventTS)))
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: HttpResponse - The gcm server encountered an error while trying to process the request for $messageId")
            }

          case Failure(e2) =>
            events.addAll(deviceIds.map(PNCallbackEvent(messageId, _, "android", "GCM_SEND_ERROR", appName, "", e2.getMessage, eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: GCM send failure for r: $messageId, e: ${e2.getMessage}", e2)
        }

        events.foreach(e => ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, e.deviceId, Channel.PUSH, e))
        ConnektLogger(LogFile.PROCESSORS).debug(s"GCMResponseHandler:: Saved callback events for $messageId ${events.toList.toString()}")

        emitMultiple[PNCallbackEvent](out,immutable.Iterable.concat(events))

        if (isAvailable(out) && !hasBeenPulled(in))
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
}
