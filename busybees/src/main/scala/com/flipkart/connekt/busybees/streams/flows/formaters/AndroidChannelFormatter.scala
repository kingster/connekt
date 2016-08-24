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
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AndroidChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, GCMPayloadEnvelope](parallelism)(ec) {

  lazy val stencilService = ServiceFactory.getStencilService

  override def map: ConnektRequest => List[GCMPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"AndroidChannelFormatter received message: ${message.toString}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      val devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceIds).get.toSeq
      val validDeviceIds = devicesInfo.map(_.deviceId)
      val invalidDeviceIds = pnInfo.deviceIds.diff(validDeviceIds.toSet)

      invalidDeviceIds.map(PNCallbackEvent(message.id, message.clientId, _, InternalStatus.MissingDeviceInfo, MobilePlatform.ANDROID, pnInfo.appName, message.contextId.orEmpty)).persist
      ServiceFactory.getReportingService.recordPushStatsDelta(message.clientId, message.contextId, message.meta.get("stencilId").map(_.toString), Option(message.platform), message.appName, InternalStatus.MissingDeviceInfo, invalidDeviceIds.size)

      val tokens = devicesInfo.map(_.token)
      val androidStencil = stencilService.getStencilsByName(s"ckt-${pnInfo.appName.toLowerCase}-android").head
      val appDataWithId = stencilService.materialize(androidStencil, message.channelData.asInstanceOf[PNRequestData].data).asInstanceOf[String].getObj[ObjectNode]
        .put("messageId", message.id)
        .put("contextId", message.contextId.orEmpty)

      val ttl = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)

      if (tokens.nonEmpty && ttl > 0) {
        val payload = GCMPNPayload(registration_ids = tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = Some(ttl), dry_run = Option(message.isTestRequest))
        List(GCMPayloadEnvelope(message.id, message.clientId, validDeviceIds, pnInfo.appName, message.contextId.orEmpty, payload, message.meta))
      } else if (tokens.nonEmpty) {
        ConnektLogger(LogFile.PROCESSORS).warn(s"AndroidChannelFormatter dropping ttl-expired message: ${message.id}")
        devicesInfo.map(d => PNCallbackEvent(message.id, message.clientId, d.deviceId, InternalStatus.TTLExpired, MobilePlatform.ANDROID, d.appName, message.contextId.orEmpty)).persist
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
