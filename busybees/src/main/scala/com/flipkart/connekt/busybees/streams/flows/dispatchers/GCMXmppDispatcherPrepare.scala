package com.flipkart.connekt.busybees.streams.flows.dispatchers

import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.MapFlowStage
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.{GCMXmppPNPayload, GcmXmppRequest, GCMPayloadEnvelope}
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 * Created by subir.dey on 28/06/16.
 */
class GCMXmppDispatcherPrepare extends MapFlowStage[GCMPayloadEnvelope, (GcmXmppRequest, GCMRequestTracker)] {

  override implicit val map: GCMPayloadEnvelope => List[(GcmXmppRequest, GCMRequestTracker)] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"GCMXmppDispatcherPrepare received message: ${message.messageId}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"GCMXmppDispatcherPrepare received message: ${message.toString}")

      KeyChainManager.getGoogleCredential(message.appName).map { credential =>
        val xmppRequest:GcmXmppRequest = GcmXmppRequest(message.gcmPayload.asInstanceOf[GCMXmppPNPayload], credential)
        val requestTrace = GCMRequestTracker(message.messageId, message.clientId, message.deviceId, message.appName, message.contextId, message.meta)
        List(xmppRequest -> requestTrace)
      }.getOrElse(List.empty[(GcmXmppRequest, GCMRequestTracker)])
    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMXmppDispatcherPrepare failed with ${e.getMessage}", e)
        throw new ConnektPNStageException(message.messageId, message.clientId, message.deviceId.toSet, InternalStatus.StageError, message.appName, MobilePlatform.ANDROID, message.contextId, message.meta, s"GCMXmppDispatcherPrepare-${e.getMessage}", e)
    }
  }
}
