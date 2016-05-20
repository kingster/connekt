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

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.MapFlowStage
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels.WNSPayloadEnvelope
import com.flipkart.connekt.commons.services.WindowsOAuthService
import com.flipkart.connekt.commons.utils.StringUtils._


class WNSDispatcherPrepare extends MapFlowStage[WNSPayloadEnvelope, (HttpRequest, WNSRequestTracker)] {

  override val map: WNSPayloadEnvelope => List[(HttpRequest, WNSRequestTracker)] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"WNSDispatcher received message: ${message.messageId}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"WNSDispatcher received message: $message")

      val bearerToken = WindowsOAuthService.getToken(message.appName.trim.toLowerCase).map(_.token).getOrElse("NO_TOKEN_AVAILABLE")
      val headers = scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "Bearer " + bearerToken),
        RawHeader("X-WNS-Type", message.wnsPayload.getType), RawHeader("X-WNS-TTL", message.time_to_live.toString)
      )

      val payload = HttpEntity(message.wnsPayload.getContentType, message.wnsPayload.getBody)
      val request = HttpRequest(HttpMethods.POST, message.token, headers, payload)

      ConnektLogger(LogFile.PROCESSORS).trace(s"WNSDispatcher prepared http request: $request")

      List((request, WNSRequestTracker(message.appName, message.messageId, message, message.meta)))

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"WNSDispatcher:: onPush :: Error", e)
        throw new ConnektPNStageException(message.messageId, Set(message.deviceId), InternalStatus.StageError, message.appName, MobilePlatform.WINDOWS, message.contextId, message.meta,s"WNSDispatcherPrepare-${e.getMessage}", e)
    }
  }
}
