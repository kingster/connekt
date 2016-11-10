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
package com.flipkart.connekt.busybees.streams.flows.formaters

import com.flipkart.connekt.busybees.models.EmailRequestTracker
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class EmailChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, (EmailPayloadEnvelope,EmailRequestTracker)](parallelism)(ec) {

  override def map: ConnektRequest => List[(EmailPayloadEnvelope,EmailRequestTracker)] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"EmailChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"EmailChannelFormatter received message: ${message.toString}")

      val emailInfo = message.channelInfo.asInstanceOf[EmailRequestInfo]

      val ttl = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)

      val cc = Option(emailInfo.cc).getOrElse(Set.empty)
      val bcc = Option(emailInfo.bcc).getOrElse(Set.empty)

      if (emailInfo.to.nonEmpty && ttl > 0) {
        val payload = EmailPayload(
          to = emailInfo.to,
          cc = cc,
          bcc = bcc,
          data = message.channelData.asInstanceOf[EmailRequestData],
          from = EmailAddress("Connekt", "connekt@flipkart.com"),
          replyTo = EmailAddress("Connekt", "connekt@flipkart.com")
        )
        List(EmailPayloadEnvelope( messageId = message.id, appName = emailInfo.appName, contextId = message.contextId.orEmpty, clientId = message.clientId, payload = payload, meta = message.meta) ->
          EmailRequestTracker ( messageId = message.id , clientId = message.clientId, to= emailInfo.to.map(_.address), cc = cc.map(_.address), appName =  emailInfo.appName, contextId = message.contextId.orEmpty, meta = message.meta) )
      } else if (emailInfo.to.nonEmpty) {
        ConnektLogger(LogFile.PROCESSORS).warn(s"EmailChannelFormatter dropping ttl-expired message: ${message.id}")
        //TODO: Fix PNCallbackEvent
        emailInfo.to.map(e => PNCallbackEvent(message.id, message.clientId, e.address, InternalStatus.TTLExpired, Channel.EMAIL, emailInfo.appName, message.contextId.orEmpty)).persist
        emailInfo.cc.map(e => PNCallbackEvent(message.id, message.clientId, e.address, InternalStatus.TTLExpired, Channel.EMAIL, emailInfo.appName, message.contextId.orEmpty)).persist
        ServiceFactory.getReportingService.recordPushStatsDelta(message.clientId, message.contextId, message.meta.get("stencilId").map(_.toString), Option(message.platform), message.appName, InternalStatus.TTLExpired, emailInfo.to.size + emailInfo.cc.size)
        List.empty
      } else
        List.empty
    } catch {
      case e: Exception =>
        //TODO: Fix PNCallbackEvent
        ConnektLogger(LogFile.PROCESSORS).error(s"EmailChannelFormatter error for ${message.id}", e)
        throw new ConnektPNStageException(message.id, message.clientId, message.deviceId, InternalStatus.StageError, message.appName, message.platform, message.contextId.orEmpty, message.meta, "AndroidChannelFormatter::".concat(e.getMessage), e)
    }
  }
}
