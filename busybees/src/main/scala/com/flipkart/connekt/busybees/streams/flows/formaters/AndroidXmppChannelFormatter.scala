package com.flipkart.connekt.busybees.streams.flows.formaters

import com.flipkart.connekt.busybees.xmpp.XmppMessageIdHelper
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.entities.DeviceDetails

import scala.concurrent.ExecutionContextExecutor
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 * Created by subir.dey on 21/06/16.
 */
class AndroidXmppChannelFormatter (parallelism: Int)(implicit ec: ExecutionContextExecutor) extends AndroidChannelFormatter(parallelism)(ec) {

  val  deliveryReceiptRequired = Some(true)

  override def formPayload(message: ConnektRequest,
                           devicesInfo:Map[String, DeviceDetails],
                           pnInfo: PNRequestInfo,
                           appDataWithId: Any,
                           timeToLive: Long,
                           dryRun: Option[Boolean]): List[GCMPayloadEnvelope] = {
    devicesInfo.map{ case (token, device ) => {
      val messageId = XmppMessageIdHelper.generateMessageId(message, device)
      val payload = GCMXmppPNPayload(token, messageId, Option(pnInfo.delayWhileIdle), appDataWithId, Some(timeToLive), deliveryReceiptRequired, dryRun)
      GCMPayloadEnvelope(message.id, message.clientId, Seq(device.deviceId), pnInfo.appName, message.contextId.orEmpty, payload, message.meta)
    }}.toList
  }
}
