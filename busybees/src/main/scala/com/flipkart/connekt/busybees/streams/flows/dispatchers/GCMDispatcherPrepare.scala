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
package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URL

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.commons.iomodels.{MessageStatus, GCMPayloadEnvelope}
import MessageStatus.InternalStatus
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.MapFlowStage
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.GCMPayloadEnvelope
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._

class GCMDispatcherPrepare(uri: URL = new URL("https", "android.googleapis.com", 443, "/gcm/send"))
  extends MapFlowStage[GCMPayloadEnvelope, (HttpRequest, GCMRequestTracker)] {

  override implicit val map: GCMPayloadEnvelope => List[(HttpRequest, GCMRequestTracker)] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare received message: ${message.messageId}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"GCMDispatcherPrepare received message: ${message.toString}")

      val requestEntity = HttpEntity(ContentTypes.`application/json`, message.gcmPayload.getJson)
      val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(message.appName).get.apiKey))
      val httpRequest = HttpRequest(HttpMethods.POST, uri.getPath, requestHeaders, requestEntity)
      val requestTrace = GCMRequestTracker(message.messageId, message.deviceId, message.appName, message.contextId, message.meta)

      List(httpRequest -> requestTrace)
    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMDispatcherPrepare failed with ${e.getMessage}", e)
        throw new ConnektPNStageException(message.messageId, message.deviceId.toSet, InternalStatus.StageError, message.appName, MobilePlatform.ANDROID, message.contextId, s"GCMDispatcherPrepare-${e.getMessage}", e)
    }
  }
}
