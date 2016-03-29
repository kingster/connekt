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

import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.MapFlowStage
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.GCMPayloadEnvelope
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._
 
class GCMDispatcherPrepare(uri: URL = new URL("https", "android.googleapis.com", 443, "/gcm/send"))
  extends MapFlowStage[GCMPayloadEnvelope, (HttpRequest, GCMRequestTracker)] {

  override implicit val map: GCMPayloadEnvelope => List[(HttpRequest, GCMRequestTracker)] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: ON_PUSH for ${message.messageId}")
      ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: onPush:: Received Message: ${message.toString}")

      val requestEntity = HttpEntity(ContentTypes.`application/json`, message.gcmPayload.getJson)
      val requestHeaders = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + KeyChainManager.getGoogleCredential(message.appName).get.apiKey))
      val httpRequest = new HttpRequest(HttpMethods.POST, uri.getPath, requestHeaders, requestEntity)
      val requestTrace = GCMRequestTracker(message.messageId, message.deviceId, message.appName)

      ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: onPush:: Request Payload : ${httpRequest.entity.asInstanceOf[Strict].data.decodeString("UTF-8")}")
      ConnektLogger(LogFile.PROCESSORS).debug(s"GCMDispatcherPrepare:: onPush:: Relayed (HttpRequest,requestTrace) to next stage for: ${requestTrace.messageId}")

      List((httpRequest, requestTrace))

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"GCMDispatcherPrepare:: onPush :: ${e.getMessage}", e)
        throw new ConnektPNStageException(message.messageId, message.deviceId, "connekt_gcmprepare_failure", message.appName, MobilePlatform.ANDROID, "TODO/Context", e.getMessage, e)
    }
  }
}
