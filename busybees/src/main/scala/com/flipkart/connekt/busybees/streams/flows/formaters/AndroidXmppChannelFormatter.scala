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

import com.flipkart.connekt.commons.helpers.XmppMessageIdHelper
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.entities.DeviceDetails

import scala.concurrent.ExecutionContextExecutor
import com.flipkart.connekt.commons.utils.StringUtils._
import scala.concurrent.duration._

class AndroidXmppChannelFormatter (parallelism: Int)(implicit ec: ExecutionContextExecutor) extends AndroidChannelFormatter(parallelism)(ec) {

  val  deliveryReceiptRequired = Some(true)

  override def createPayload(message: ConnektRequest,
                           devicesInfo:Seq[DeviceDetails],
                           appDataWithId: Any): List[GCMPayloadEnvelope] = {

    val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
    val timeToLive = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)
    val dryRun = message.meta.get("x-perf-test").map(v => v.trim.equalsIgnoreCase("true"))

    devicesInfo.map{ device => {
      val messageId = XmppMessageIdHelper.generateMessageId(message, device.deviceId)
      val payload = GCMXmppPNPayload(device.token, messageId, Option(pnInfo.delayWhileIdle), appDataWithId, Some(timeToLive), deliveryReceiptRequired, dryRun)
      GCMPayloadEnvelope(message.id, message.clientId, Seq(device.deviceId), pnInfo.appName, message.contextId.orEmpty, payload, message.meta)
    }}.toList
  }
}
