package com.flipkart.connekt.busybees.streams.flows.formaters

import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class AndroidChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, GCMPayloadEnvelope](parallelism)(ec) {

  override def map: ConnektRequest => List[GCMPayloadEnvelope] = message  => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelFormatter:: onPush:: Received Message: ${message.getJson}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
      val tokens = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None)).map(_.token)

      val appDataWithId = message.channelData.asInstanceOf[PNRequestData].data.put("messageId", message.id)
      val dryRun = message.meta.get("x-perf-test").map(v => v.trim.equalsIgnoreCase("true"))

      tokens.map(t => pnInfo.platform.toUpperCase match {
        case "ANDROID" => GCMPNPayload(registration_ids = tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = None, dry_run = dryRun)
        case "OPENWEB" => OpenWebGCMPayload(registration_ids = tokens, dry_run = None)
      }).map(GCMPayloadEnvelope(message.id, pnInfo.deviceId, pnInfo.appName, _))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"AndroidChannelFormatter:: OnFormat error", e)
        List.empty[GCMPayloadEnvelope]
    }

  }
}
