package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.stream.Materializer
import com.flipkart.connekt.commons.iomodels.{PNCallbackEvent, XmppReceipt, XmppUpstreamData}

import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.metrics.Instrumented
import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by subir.dey on 26/06/16.
 */
class XmppUpstreamHandler (implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[XmppUpstreamData](96) with Instrumented {

  override val map: (XmppUpstreamData) => Future[List[PNCallbackEvent]] = upstreamResponse => Future(profile("map") {
    val eventType: String = upstreamResponse.data.get("eventType").asInstanceOf[String]

    val events = if (eventType != null) {
          val messageId = Option(upstreamResponse.data.get("messageId").asInstanceOf[String]).getOrElse("Unknown")
          val epochTime = Option(upstreamResponse.data.get("timestamp").asInstanceOf[String]).map(_.toLong).getOrElse(System.currentTimeMillis)

          List(PNCallbackEvent(messageId = messageId,
            clientId = upstreamResponse.category,
            deviceId = Option(upstreamResponse.data.get("deviceId").asInstanceOf[String]).getOrElse("Unknown"),
            eventType = eventType,
            platform = "android",
            appName = Option(upstreamResponse.data.get("appName").asInstanceOf[String]).getOrElse("NA"),
            contextId = Option(upstreamResponse.data.get("contextId").asInstanceOf[String]).getOrElse(""),
            cargo = Option(upstreamResponse.data.get("cargo").asInstanceOf[String]).getOrElse(""),
            timestamp = epochTime
          ))
        } else List()
    events.persist
    events
  })(m.executionContext)
}
