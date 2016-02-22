package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import akka.util.{ByteString, ByteStringBuilder}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.streams.flows.dispatchers.RequestIdentifier
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ServiceFactory, ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class GCMResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[HttpResponse], RequestIdentifier)] {

  val in = Inlet[(Try[HttpResponse], RequestIdentifier)]("GCMResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("GCMResponseHandler.Out")

  override def shape  = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val gcmResponse = grab(in)
        val id = gcmResponse._2.messageId
        val app = gcmResponse._2.appName
        val deviceIds = gcmResponse._2.deviceId

        val events = ListBuffer[PNCallbackEvent]()
        val eventTS = System.currentTimeMillis()

        gcmResponse._1 match {
          case Success(r) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Received httpResponse for r: $id")
            r.status.intValue() match {
              case 200 =>
                r.entity.dataBytes.runFold[ByteStringBuilder](ByteString.newBuilder)((u, bs) => {u ++= bs}).onComplete {
                  case Success(b) =>
                    ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: RESPONSE: ${b.result().decodeString("UTF-8")}")
                    val responseBody = b.result().decodeString("UTF-8").getObj[ObjectNode]

                    val deviceIdItr = deviceIds.listIterator()
                    responseBody.findValues("results").toList.foreach({
                      case s if s.has("message_id") => events += PNCallbackEvent(id, deviceId = deviceIdItr.next(), platform = "android", eventType = "GCM_RECEIVED", appName = app, contextId = "", cargo = s.get("message_id").asText(), timestamp = eventTS)
                      case f if f.has("error") => events += PNCallbackEvent(id, deviceId = deviceIdItr.next(), platform = "android", eventType = "GCM_ERROR", appName = app, contextId = "", cargo = f.get("error").asText(), timestamp = eventTS)
                    })

                    ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: ResponseBody:: $responseBody")
                  case Failure(e1) =>
                    events.addAll(deviceIds.map(PNCallbackEvent(id, _, "android", "GCM_RESPONSE_PARSE_ERROR", app, "", e1.getMessage, eventTS)))
                    ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: Error Processing ResponseBody for $id:: ${e1.getMessage}", e1)
                }
              case 400 =>
                events.addAll(deviceIds.map(PNCallbackEvent(id, _, "android", "GCM_INVALID_JSON", app, "", "", eventTS)))
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Invalid JSON Sent for $id")
              case 401 =>
                events.addAll(deviceIds.map(PNCallbackEvent(id, _, "android", "GCM_AUTH_ERROR", app, "", "", eventTS)))
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The sender account used to send a message couldn't be authenticated. for $id")
              case w if 5 == (w/100) =>
                events.addAll(deviceIds.map(PNCallbackEvent(id, _, "android", "GCM_INTERNAL_ERROR", app, "", "", eventTS)))
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The gcm server encountered an error while trying to process the request for $id")
            }

          case Failure(e2) =>
            events.addAll(deviceIds.map(PNCallbackEvent(id, _, "android", "GCM_SEND_ERROR", app, "", e2.getMessage, eventTS)))
            ConnektLogger(LogFile.PROCESSORS).error(s"Sink:: Received Error for r: $id, e: ${e2.getMessage}", e2)
        }

        events.foreach(e => ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, e.deviceId, Channel.PUSH, e))
        ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Saved callback events for $id ${events.toList.toString()}")

        emitMultiple[PNCallbackEvent](out,immutable.Iterable.concat(events))
      } catch {
        case e:Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: onPush :: Error: ${e.getMessage}", e)
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }
    })

  }

}
