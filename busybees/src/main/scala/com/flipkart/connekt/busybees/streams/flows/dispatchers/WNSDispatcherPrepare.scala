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

import java.net.URI

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.streams.MapFlowStage
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.WNSPayloadEnvelope
import com.flipkart.connekt.commons.services.WindowsOAuthService
import com.flipkart.connekt.commons.utils.StringUtils._


class WNSDispatcherPrepare extends MapFlowStage[WNSPayloadEnvelope, (HttpRequest, WNSRequestTracker)] {

  override val map: WNSPayloadEnvelope => List[(HttpRequest, WNSRequestTracker)] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"WNSDispatcher:: ON_PUSH for ${message.messageId}")
      ConnektLogger(LogFile.PROCESSORS).info(s"WNSDispatcher:: onPush:: Received Message: $message")

      val uri = new URI(message.token).toURL
      val bearerToken = WindowsOAuthService.getToken(message.appName).map(_.token).getOrElse("INVALID")
      val headers = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "Bearer " + bearerToken),
        RawHeader("X-WNS-Type", message.wnsPayload.getType), RawHeader("X-WNS-TTL", message.time_to_live.toString)
      )

      val payload = HttpEntity(message.wnsPayload.getContentType, message.wnsPayload.getBody)
      val request = new HttpRequest(HttpMethods.POST, uri.getFile, headers, payload)

      List((request, WNSRequestTracker(message.appName, message.messageId, message)))

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"WNSDispatcher:: onPush :: Error", e)
        throw new ConnektPNStageException(message.messageId, List(message.deviceId), "connekt_gcmprepare_failure", message.appName, MobilePlatform.ANDROID, "TODO/Context", e.getMessage, e)
    }
  }
}
