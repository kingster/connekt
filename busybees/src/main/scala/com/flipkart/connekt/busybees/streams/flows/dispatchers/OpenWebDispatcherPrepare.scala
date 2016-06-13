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
import com.flipkart.connekt.busybees.models.OpenWebRequestTracker
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.MapFlowStage
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels.OpenWebStandardPayloadEnvelope
import com.flipkart.connekt.commons.utils.StringUtils._

class OpenWebDispatcherPrepare extends MapFlowStage[OpenWebStandardPayloadEnvelope, (HttpRequest, OpenWebRequestTracker)] {

  override implicit val map: OpenWebStandardPayloadEnvelope => List[(HttpRequest, OpenWebRequestTracker)] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"OpenWebDispatcherPrepare received message: ${message.messageId}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"OpenWebDispatcherPrepare received message: ${message.toString}")

      val requestEntity = HttpEntity(ContentTypes.`application/octet-stream`, message.payload.data)

      val requestHeaders: scala.collection.immutable.Seq[HttpHeader] = scala.collection.immutable.Seq(message.headers.map { case (key, value) => RawHeader(key, value).asInstanceOf[HttpHeader] }.toList: _*)
      val httpRequest = HttpRequest(HttpMethods.POST, message.providerUrl, requestHeaders, requestEntity)
      val requestTrace = OpenWebRequestTracker(message.messageId, message.deviceId, message.appName, message.contextId, message.client, message.meta)

      List(httpRequest -> requestTrace)
    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebDispatcherPrepare failed with ${e.getMessage}", e)
        throw new ConnektPNStageException(message.messageId, message.client, Set(message.deviceId), InternalStatus.StageError, message.appName, MobilePlatform.OPENWEB, message.contextId, message.meta, s"OpenWebDispatcherPrepare-${e.getMessage}", e)
    }
  }
}
