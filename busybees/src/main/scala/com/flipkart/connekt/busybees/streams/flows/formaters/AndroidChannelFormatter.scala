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
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.{DeviceDetailsService, PNPlatformStencilService, StencilService}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AndroidChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, GCMPayloadEnvelope](parallelism)(ec) {

  override def map: ConnektRequest => List[GCMPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"AndroidChannelFormatter received message: ${message.toString}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      val devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceIds).get.toSeq
      val validDeviceIds = devicesInfo.map(_.deviceId)
      val invalidDeviceIds = pnInfo.deviceIds.diff(validDeviceIds.toSet)
      invalidDeviceIds.map(PNCallbackEvent(message.id, message.clientId, _, InternalStatus.MissingDeviceInfo, MobilePlatform.ANDROID, pnInfo.appName, message.contextId.orEmpty)).persist

      val tokens = devicesInfo.map(_.token)
      val androidStencil = StencilService.get(s"ckt-${pnInfo.appName.toLowerCase}-android").get

      val appDataWithId = PNPlatformStencilService.getPNData(androidStencil, message.channelData.asInstanceOf[PNRequestData].data).getObj[ObjectNode].put("messageId", message.id)
      val dryRun = message.meta.get("x-perf-test").map(v => v.trim.equalsIgnoreCase("true"))
      val ttl = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)

      if (tokens.nonEmpty && ttl > 0) {
        val payload = GCMPNPayload(registration_ids = tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = Some(ttl), dry_run = dryRun)
        List(GCMPayloadEnvelope(message.id, message.clientId, validDeviceIds, pnInfo.appName, message.contextId.orEmpty, payload, message.meta))
      } else if (tokens.nonEmpty) {
        ConnektLogger(LogFile.PROCESSORS).warn(s"AndroidChannelFormatter dropping ttl-expired message: ${message.id}")
        devicesInfo.map(d => PNCallbackEvent(message.id, message.clientId, d.deviceId, InternalStatus.TTLExpired, MobilePlatform.ANDROID, d.appName, message.contextId.orEmpty)).persist
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
