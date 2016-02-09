package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import akka.util.{ByteString, ByteStringBuilder}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
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
class GCMResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[HttpResponse], String)] {

  val in = Inlet[(Try[HttpResponse], String)]("GCMResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("GCMResponseHandler.Out")

  override def shape  = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val gcmResponse = grab(in)

        val events = ListBuffer[PNCallbackEvent]()
        val eventTS = System.currentTimeMillis()

        gcmResponse._1 match {
          case Success(r) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Received httpResponse for r: ${gcmResponse._2}")
            r.status.intValue() match {
              case 200 =>
                r.entity.dataBytes.runFold[ByteStringBuilder](ByteString.newBuilder)((u, bs) => {u ++= bs}).onComplete {
                  case Success(b) =>
                    val responseBody = b.result().decodeString("UTF-8").getObj[ObjectNode]
                    responseBody.findValues("results").toList.foreach({
                      case s if s.has("message_id") => events += PNCallbackEvent(messageId = gcmResponse._2, deviceId = "", platform = "android", eventType = "GCM_RECEIVED", appName = "", contextId = "", cargo = s.get("message_id").asText(), timestamp = eventTS)
                      case f if f.has("error") => events += PNCallbackEvent(messageId = gcmResponse._2, deviceId = "", platform = "android", eventType = "GCM_ERROR", appName = "", contextId = "", cargo = f.get("error").asText(), timestamp = eventTS)
                    })

                    ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: ResponseBody:: $responseBody")
                  case Failure(e1) =>
                    events += PNCallbackEvent(messageId = gcmResponse._2, deviceId = "", platform = "android", eventType = "GCM_RESPONSE_PARSE_ERROR", appName = "", contextId = "", cargo = e1.getMessage, timestamp = eventTS)
                    ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: Error Processing ResponseBody:: ${e1.getMessage}", e1)
                }
              case 400 =>
                events += PNCallbackEvent(messageId = gcmResponse._2, deviceId = "", platform = "android", eventType = "GCM_INVALID_JSON", appName = "", contextId = "", cargo = gcmResponse._2, timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: Invalid JSON Sent.")
              case 401 =>
                events += PNCallbackEvent(messageId = gcmResponse._2, deviceId = "", platform = "android", eventType = "GCM_AUTH_ERROR", appName = "", contextId = "", cargo = gcmResponse._2, timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The sender account used to send a message couldn't be authenticated.")
              case w if 5 == (w/100) =>
                events += PNCallbackEvent(messageId = gcmResponse._2, deviceId = "", platform = "android", eventType = "GCM_INTERNAL_ERROR", appName = "", contextId = "", cargo = gcmResponse._2, timestamp = eventTS)
                ConnektLogger(LogFile.PROCESSORS).info(s"GCMResponseHandler:: The gcm server encountered an error while trying to process the request.")
            }

          case Failure(e2) =>
            events += PNCallbackEvent(messageId = gcmResponse._2, deviceId = "", platform = "android", eventType = "GCM_SEND_ERROR", appName = "", contextId = "", cargo = e2.getMessage, timestamp = eventTS)
            ConnektLogger(LogFile.PROCESSORS).error(s"Sink:: Received Error for r: ${gcmResponse._2}, e: ${e2.getMessage}", e2)
        }


        emitMultiple[PNCallbackEvent](out,immutable.Iterable.concat(events))
      }catch {
        case e:Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"GCMResponseHandler:: onPush :: Error", e)
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
