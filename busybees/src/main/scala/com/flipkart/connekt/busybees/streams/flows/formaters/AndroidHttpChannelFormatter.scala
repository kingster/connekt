package com.flipkart.connekt.busybees.streams.flows.formaters

import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor

/**
 * Created by subir.dey on 21/06/16.
 */
class AndroidHttpChannelFormatter (parallelism: Int)(implicit ec: ExecutionContextExecutor) extends AndroidChannelFormatter(parallelism)(ec) {

  override def formPayload(message:ConnektRequest,
                           devicesInfo:Seq[DeviceDetails],
                           pnInfo:PNRequestInfo,
                           appDataWithId:Any,
                           timeToLive:Long,
                           dryRun:Option[Boolean]):List[GCMPayloadEnvelope] = {
    val tokens = devicesInfo.map(_.token)
    val validDeviceIds = devicesInfo.map(_.deviceId)

    val payload = GCMHttpPNPayload(registration_ids = tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = Some(timeToLive), dry_run = dryRun)
    List(GCMPayloadEnvelope(message.id, message.clientId, validDeviceIds, pnInfo.appName, message.contextId.orEmpty, payload, message.meta))
  }
}
