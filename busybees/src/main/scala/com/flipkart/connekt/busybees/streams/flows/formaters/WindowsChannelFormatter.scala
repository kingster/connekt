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
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

/**
 * @author aman.shrivastava on 08/02/16.
 */
class WindowsChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, WNSPayloadEnvelope](parallelism)(ec) {

  override def map: (ConnektRequest) => List[WNSPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter:: onPush:: Received Message: ${message.getJson}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
      val payload = message.channelData.asInstanceOf[PNRequestData].data
      val wnsPayload = makeWNSPayload("toast" /* wnsPayload `type` has to be dynamically available */, payload).get

      val devices = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None))
      ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter:: onPush:: devices: ${devices.getJson}")

      val wnsRequestEnvelopes = devices.map(d => WNSPayloadEnvelope(message.id, d.token, message.channelInfo.asInstanceOf[PNRequestInfo].appName, d.deviceId, wnsPayload))

      if(wnsRequestEnvelopes.nonEmpty) {
        val dryRun = message.meta.get("x-perf-test").exists(_.trim.equalsIgnoreCase("true"))
        if (!dryRun) {
          ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter:: PUSHED downstream for ${message.id}")
          wnsRequestEnvelopes
        } else {
          ConnektLogger(LogFile.PROCESSORS).debug(s"WindowsChannelFormatter:: Dry Run Dropping msgId: ${message.id}")
          List.empty[WNSPayloadEnvelope]
        }
      } else {
        ConnektLogger(LogFile.PROCESSORS).warn(s"WindowsChannelFormatter:: No Device Details found for : ${pnInfo.deviceId}, msgId: ${message.id}")
        List.empty[WNSPayloadEnvelope]
      }
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"WindowsChannelFormatter:: OnFormat error", e)
        List.empty[WNSPayloadEnvelope]
    }
  }

  private def makeWNSPayload(notificationType: String, input: ObjectNode) = Try {
    WindowsNotificationType.withName(notificationType) match {
      case WindowsNotificationType.toast => WNSToastPayload(input.get("title").asText(), input.get("message").asText(), input.get("actions").asInstanceOf[ObjectNode])
      case WindowsNotificationType.tile => WNSTilePayload(input.get("title").asText(), input.get("message").asText(), input.get("actions").asInstanceOf[ObjectNode])
      case WindowsNotificationType.badge => WNSBadgePayload(input.get("title").asText(), input.get("message").asText(), input.get("actions").asInstanceOf[ObjectNode])
      case WindowsNotificationType.raw => WNSRawPayload(input.get("title").asText(), input.get("message").asText(), input.get("actions").asInstanceOf[ObjectNode])
    }
  }
}
