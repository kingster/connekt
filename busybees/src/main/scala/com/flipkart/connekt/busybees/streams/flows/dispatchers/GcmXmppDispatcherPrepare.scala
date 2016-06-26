package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.http.scaladsl.model._
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.MapFlowStage
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.{GCMPayload, XmppRequest, GCMPayloadEnvelope}
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 * Created by subir.dey on 21/06/16.
 */
class GcmXmppDispatcherPrepare extends MapFlowStage[GCMPayloadEnvelope, (String, GCMRequestTracker)] {

  override implicit val map: GCMPayloadEnvelope => List[(String, GCMRequestTracker)] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare received message: ${message.messageId}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"GCMDispatcherPrepare received message: ${message.toString}")

      val xmppRequest:String = message.gcmPayload.getJson
      val requestTrace = GCMRequestTracker(message.messageId, message.clientId, message.deviceId, message.appName, message.contextId, message.meta)

      List(xmppRequest -> requestTrace)
    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMDispatcherPrepare failed with ${e.getMessage}", e)
        throw new ConnektPNStageException(message.messageId, message.clientId, message.deviceId.toSet, InternalStatus.StageError, message.appName, MobilePlatform.ANDROID, message.contextId, message.meta, s"GCMDispatcherPrepare-${e.getMessage}", e)
    }
  }
}
