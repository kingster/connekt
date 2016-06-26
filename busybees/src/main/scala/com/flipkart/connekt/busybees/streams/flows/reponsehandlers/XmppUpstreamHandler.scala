package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.stream.Materializer
import com.flipkart.connekt.commons.iomodels.{PNCallbackEvent, XmppReceipt, XmppUpstreamData}

import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.metrics.Instrumented
import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by subir.dey on 26/06/16.
 */
class XmppUpstreamHandler (implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[Either[XmppUpstreamData, XmppReceipt]](96) with Instrumented {

  override val map: (Either[XmppUpstreamData, XmppReceipt]) => Future[List[PNCallbackEvent]] = upstreamResponse => Future(profile("map") {

    val events = upstreamResponse match {
      case Left(upstream) => {
        val eventType: String = upstream.data.get("eventType").asInstanceOf[String]

        if (eventType != null) {
          val messageId = Option(upstream.data.get("messageId").asInstanceOf[String]).getOrElse("Unknown")
          val epochTime = Option(upstream.data.get("timestamp").asInstanceOf[String]).map(_.toLong).getOrElse(System.currentTimeMillis)

          List(PNCallbackEvent(messageId = messageId,
            clientId = upstream.category,
            deviceId = Option(upstream.data.get("deviceId").asInstanceOf[String]).getOrElse("Unknown"),
            eventType = eventType,
            platform = "android",
            appName = Option(upstream.data.get("appName").asInstanceOf[String]).getOrElse("NA"),
            contextId = Option(upstream.data.get("contextId").asInstanceOf[String]).getOrElse(""),
            cargo = Option(upstream.data.get("cargo").asInstanceOf[String]).getOrElse(""),
            timestamp = epochTime
          ))
        } else List()
      }
      case Right(receipt) => {
        List(PNCallbackEvent(messageId = receipt.data.originalMessageId,
          clientId = receipt.from,
          deviceId = receipt.data.deviceRegistrationId,
          eventType = receipt.responseType(),
          platform = "android",
          appName = receipt.category,
          contextId = "",
          cargo = "",
          timestamp = System.currentTimeMillis
        ))
      }
    }
    events.persist
    events
  })(m.executionContext)
}
