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
import com.flipkart.connekt.commons.services.{DeviceDetailsService, StencilService}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class IOSChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, APSPayloadEnvelope](parallelism)(ec) {

  override def map: (ConnektRequest) => List[APSPayloadEnvelope] = message => {
    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"IOSChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"IOSChannelFormatter received message: ${message.getJson}")
      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      val devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceIds).get
      val invalidDeviceIds = pnInfo.deviceIds.diff(devicesInfo.map(_.deviceId).toSet)
      invalidDeviceIds.map(PNCallbackEvent(message.id, message.client, _, InternalStatus.MissingDeviceInfo, MobilePlatform.IOS, pnInfo.appName, message.contextId.orEmpty)).persist

      val listOfTokenDeviceId = devicesInfo.map(r => (r.token, r.deviceId))
      val iosStencil = StencilService.getStencilByName(s"ckt-${pnInfo.appName.toLowerCase}-ios").get


      val ttlInMillis = message.expiryTs.getOrElse(System.currentTimeMillis() + 6.hours.toMillis)
      val apnsEnvelopes = listOfTokenDeviceId.map(td => {
        val data = message.channelData.asInstanceOf[PNRequestData].data
        val requestData = StencilService.render(iosStencil.find(_.component.equals("data")).orNull, data)
        val apnsTopic = pnInfo.topic.getOrElse(StencilService.render(iosStencil.find(_.component.equals("topic")).orNull, data))
        val apnsPayload = iOSPNPayload(td._1, apnsTopic, ttlInMillis, requestData)
        APSPayloadEnvelope(message.id, td._2, pnInfo.appName, message.contextId.orEmpty, message.client, apnsPayload, message.meta)
      })

      if (apnsEnvelopes.nonEmpty && ttlInMillis > System.currentTimeMillis()) {
        val dryRun = message.meta.get("x-perf-test").exists(_.trim.equalsIgnoreCase("true"))
        if (!dryRun) {
          ConnektLogger(LogFile.PROCESSORS).trace(s"IOSChannelFormatter pushed downstream for: ${message.id}")
          apnsEnvelopes
        }
        else {
          ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter dropping dry-run message: ${message.id}")
          List.empty[APSPayloadEnvelope]
        }
      } else if (apnsEnvelopes.nonEmpty) {
        ConnektLogger(LogFile.PROCESSORS).warn(s"IOSChannelFormatter dropping ttl-expired message: ${message.id}")
        apnsEnvelopes.map(e => PNCallbackEvent(e.messageId, message.client, e.deviceId, InternalStatus.TTLExpired, MobilePlatform.IOS, e.appName, message.contextId.orEmpty)).persist
        List.empty[APSPayloadEnvelope]
      } else
        List.empty[APSPayloadEnvelope]

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"IOSChannelFormatter error for ${message.id}", e)
        throw new ConnektPNStageException(message.id, message.client, message.deviceId, InternalStatus.StageError, message.appName, message.platform, message.contextId.orEmpty, message.meta, "IOSChannelFormatter::".concat(e.getMessage), e)
    }
  }
}
