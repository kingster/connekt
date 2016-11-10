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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.entities.{Channel, MobilePlatform}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class EmailChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, EmailPayloadEnvelope](parallelism)(ec) {

  override def map: ConnektRequest => List[EmailPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"EmailChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"EmailChannelFormatter received message: ${message.toString}")

      val emailInfo = message.channelInfo.asInstanceOf[EmailRequestInfo]


      val ttl = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)

      if (emailInfo.to.nonEmpty && ttl > 0) {
        val payload = GCMPNPayload(registration_ids = tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = Some(ttl), dry_run = Option(message.isTestRequest))
        List(GCMPayloadEnvelope(message.id, message.clientId, validDeviceIds, pnInfo.appName, message.contextId.orEmpty, payload, message.meta))
      } else if (emailInfo.to.nonEmpty) {
        ConnektLogger(LogFile.PROCESSORS).warn(s"EmailChannelFormatter dropping ttl-expired message: ${message.id}")
        emailInfo.to.map(e => PNCallbackEvent(message.id, message.clientId, e.address, InternalStatus.TTLExpired, Channel.EMAIL, emailInfo.appName, message.contextId.orEmpty)).persist
        ServiceFactory.getReportingService.recordPushStatsDelta(message.clientId, message.contextId, message.meta.get("stencilId").map(_.toString), Option(message.platform), message.appName, InternalStatus.TTLExpired, devicesInfo.size)
        List.empty[GCMPayloadEnvelope]
      } else
        List.empty[GCMPayloadEnvelope]
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"AndroidChannelFormatter error for ${message.id}", e)
        throw new ConnektPNStageException(message.id, message.clientId, message.deviceId, InternalStatus.StageError, message.appName, message.platform, message.contextId.orEmpty, message.meta, "AndroidChannelFormatter::".concat(e.getMessage), e)
    }
  }
}
