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
import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.{DeviceDetailsService, PNStencilService, StencilService}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AndroidChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, GCMPayloadEnvelope](parallelism)(ec) {

  override def map: ConnektRequest => List[GCMPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelFormatter:: onPush:: Received Message: ${message.getJson}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      val devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceId).get
      val invalidDeviceIds = pnInfo.deviceId.diff(devicesInfo.map(_.deviceId))
      invalidDeviceIds.map(PNCallbackEvent(message.id, _, "INVALID_DEVICE_ID", MobilePlatform.ANDROID, pnInfo.appName, message.contextId.orEmptyString, "")).persist

      val tokens = devicesInfo.map(_.token)
      val androidStencil = StencilService.get(s"ckt-${pnInfo.appName.toLowerCase}-android").get

      val appDataWithId = PNStencilService.getPNData(androidStencil, message.channelData.asInstanceOf[PNRequestData].data).getObj[ObjectNode].put("messageId", message.id)
      val dryRun = message.meta.get("x-perf-test").map(v => v.trim.equalsIgnoreCase("true"))
      val ttl = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)

      if (ttl > 0) {
        tokens.map(t => pnInfo.platform.toUpperCase match {
          case "ANDROID" => GCMPNPayload(registration_ids = tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = Some(ttl), dry_run = dryRun)
          case "OPENWEB" => OpenWebGCMPayload(registration_ids = tokens, dry_run = None)
        }).map(GCMPayloadEnvelope(message.id, pnInfo.deviceId, pnInfo.appName, _))
      } else {
        ConnektLogger(LogFile.PROCESSORS).warn(s"AndroidChannelFormatter:: Dropped message since expired.")
        List.empty[GCMPayloadEnvelope] //dropping the PN, its expiry is in past
      }
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"AndroidChannelFormatter:: OnFormat error", e)
        List.empty[GCMPayloadEnvelope]
    }

  }
}
