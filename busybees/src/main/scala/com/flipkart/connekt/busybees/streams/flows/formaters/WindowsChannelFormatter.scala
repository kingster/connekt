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

class WindowsChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, WNSPayloadEnvelope](parallelism)(ec) {

  lazy val stencilService = ServiceFactory.getStencilService

  override def map: (ConnektRequest) => List[WNSPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"WindowsChannelFormatter received message: ${message.getJson}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      val devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceIds).get
      val invalidDeviceIds = pnInfo.deviceIds.diff(devicesInfo.map(_.deviceId).toSet)

      invalidDeviceIds.map(PNCallbackEvent(message.id, message.clientId, _, InternalStatus.MissingDeviceInfo, MobilePlatform.WINDOWS, pnInfo.appName, message.contextId.orEmpty)).persist

      val (validDevices, invalidTokenDevices) = devicesInfo.partition(_.token.isValidUrl)

      invalidTokenDevices
        .map(d => PNCallbackEvent(message.id, message.clientId, d.deviceId, InternalStatus.InvalidToken, MobilePlatform.WINDOWS, pnInfo.appName, message.contextId.orEmpty))
        .persist

      val windowsStencil = stencilService.getStencilsByName(s"ckt-${pnInfo.appName.toLowerCase}-windows").head
      val ttlInSeconds = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hours.toSeconds)

      val wnsRequestEnvelopes = validDevices.map(d => {
        val wnsPayload = WNSToastPayload(stencilService.materialize(windowsStencil, message.channelData.asInstanceOf[PNRequestData].data).asInstanceOf[String])
        WNSPayloadEnvelope(message.id, message.clientId, d.token, message.channelInfo.asInstanceOf[PNRequestInfo].appName, d.deviceId, ttlInSeconds, message.contextId.orEmpty, wnsPayload, message.meta)
      })

      if (wnsRequestEnvelopes.nonEmpty && ttlInSeconds > 0) {
        val dryRun = message.meta.get("x-perf-test").exists(_.trim.equalsIgnoreCase("true"))
        if (!dryRun) {
          ConnektLogger(LogFile.PROCESSORS).trace(s"WindowsChannelFormatter pushed downstream for: ${message.id}")
          wnsRequestEnvelopes
        } else {
          ConnektLogger(LogFile.PROCESSORS).debug(s"WindowsChannelFormatter dropping dry-run message: ${message.id}")
          List.empty[WNSPayloadEnvelope]
        }
      } else if (wnsRequestEnvelopes.nonEmpty) {
        ConnektLogger(LogFile.PROCESSORS).warn(s"WindowsChannelFormatter dropping ttl-expired message: ${message.id}")
        wnsRequestEnvelopes.map(w => PNCallbackEvent(w.messageId, message.clientId, w.deviceId, InternalStatus.TTLExpired, MobilePlatform.WINDOWS, pnInfo.appName, message.contextId.orEmpty)).persist
        List.empty[WNSPayloadEnvelope]
      } else
        List.empty[WNSPayloadEnvelope]
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"WindowsChannelFormatter error for message: ${message.id}", e)
        throw new ConnektPNStageException(message.id, message.clientId, message.deviceId, InternalStatus.StageError, message.appName, message.platform, message.contextId.orEmpty, message.meta, "WindowsChannelFormatter::".concat(e.getMessage), e)
    }
  }
}
