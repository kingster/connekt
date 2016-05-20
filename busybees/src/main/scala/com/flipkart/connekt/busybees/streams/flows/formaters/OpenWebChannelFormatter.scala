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
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
 * This is a dummy impl of open-web, which runs via gcm without payload support for now.
 * @param parallelism
 * @param ec
 */
class OpenWebChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, PayloadEnvelope](parallelism)(ec) {

  override def map: ConnektRequest => List[PayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"OpenWebChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"OpenWebChannelFormatter received message: ${message.toString}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      val devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceIds).get.toSeq
      val validDeviceIds = devicesInfo.map(_.deviceId)
      val invalidDeviceIds = pnInfo.deviceIds.diff(validDeviceIds.toSet)
      invalidDeviceIds.map(PNCallbackEvent(message.id, _, InternalStatus.MissingDeviceInfo, MobilePlatform.OPENWEB, pnInfo.appName, message.contextId.orEmpty)).persist

      //TODO : Output correctly
      //the original token is a url, take the last part for chrome as gcm token id. Ref : https://developers.google.com/web/updates/2015/03/push-notifications-on-the-open-web
      val tokens = devicesInfo.map(_.token.split('/').last)
      //val androidStencil = StencilService.get(s"ckt-${pnInfo.appName.toLowerCase}-android").get

      //No Data Support for now. TODO : Add support
      //val appDataWithId = PNPlatformStencilService.getPNData(androidStencil, message.channelData.asInstanceOf[PNRequestData].data).getObj[ObjectNode].put("messageId", message.id)
      val dryRun = message.meta.get("x-perf-test").map(v => v.trim.equalsIgnoreCase("true"))
      val ttl = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)

      if (tokens.nonEmpty && ttl > 0) {
        val payload = OpenWebGCMPayload(registration_ids = tokens, dry_run = dryRun)
        List(GCMPayloadEnvelope(message.id,validDeviceIds, pnInfo.appName, message.contextId.orEmpty , payload, message.meta))
      } else if (tokens.nonEmpty){
        ConnektLogger(LogFile.PROCESSORS).warn(s"OpenWebChannelFormatter dropping ttl-expired message: ${message.id}")
        devicesInfo.map(d => PNCallbackEvent(message.id, d.deviceId, InternalStatus.TTLExpired, MobilePlatform.ANDROID, d.appName, message.contextId.orEmpty)).persist
        List.empty[GCMPayloadEnvelope]
      } else
        List.empty[OpenWebPayloadEnvelope]
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebChannelFormatter error for ${message.id}", e)
        throw new ConnektPNStageException(message.id, message.deviceId, InternalStatus.StageError, message.appName, message.platform, message.contextId.orEmpty, message.meta, "AndroidChannelFormatter::".concat(e.getMessage), e)
    }
  }
}
