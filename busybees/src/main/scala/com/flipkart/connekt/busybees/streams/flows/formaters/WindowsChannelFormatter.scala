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

import com.flipkart.connekt.busybees.models.MessageStatus.InternalStatus
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.{DeviceDetailsService, PNStencilService, StencilService}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import org.apache.commons.validator.UrlValidator

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class WindowsChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, WNSPayloadEnvelope](parallelism)(ec) {

  override def map: (ConnektRequest) => List[WNSPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"WindowsChannelFormatter received message: ${message.getJson}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      var devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceIds).get
      val invalidDeviceIds = pnInfo.deviceIds.diff(devicesInfo.map(_.deviceId).toSet)

      invalidDeviceIds.map(PNCallbackEvent(message.id, _, InternalStatus.MissingDeviceInfo, MobilePlatform.WINDOWS, pnInfo.appName, message.contextId.orEmpty)).persist
      val schemes = Array("http", "https")
      val invalidTokenDevices = devicesInfo.filter(d => {
        val urlValidator: UrlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_ALL_SCHEMES);
        if (urlValidator.isValid(d.token))
          false
        else
          true
      })
      invalidTokenDevices.map(d => PNCallbackEvent(message.id, d.deviceId, InternalStatus.InvalidTokenDevice, MobilePlatform.WINDOWS, pnInfo.appName, message.contextId.orEmpty).persist)

      devicesInfo = devicesInfo.diff(invalidTokenDevices)

      val windowsStencil = StencilService.get(s"ckt-${pnInfo.appName.toLowerCase}-windows").get
      val ttlInSeconds = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hours.toSeconds)

      val wnsRequestEnvelopes = devicesInfo.map(d => {
        val wnsPayload = WNSToastPayload(PNStencilService.getPNData(windowsStencil, message.channelData.asInstanceOf[PNRequestData].data))
        WNSPayloadEnvelope(message.id, d.token, message.channelInfo.asInstanceOf[PNRequestInfo].appName, d.deviceId, ttlInSeconds, message.contextId.orEmpty, wnsPayload)
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
        wnsRequestEnvelopes.map(w => PNCallbackEvent(w.messageId, w.deviceId, InternalStatus.TTLExpired, MobilePlatform.WINDOWS, pnInfo.appName, message.contextId.orEmpty)).persist
        List.empty[WNSPayloadEnvelope]
      } else
        List.empty[WNSPayloadEnvelope]
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"WindowsChannelFormatter error for message: ${message.id}", e)
        throw new ConnektPNStageException(message.id, message.deviceId, InternalStatus.StageError, message.appName, message.platform, message.contextId.orEmpty, "WindowsChannelFormatter::".concat(e.getMessage), e)
    }
  }
}
