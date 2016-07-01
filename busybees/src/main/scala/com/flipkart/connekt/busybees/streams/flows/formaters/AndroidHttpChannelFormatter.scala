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

import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AndroidHttpChannelFormatter (parallelism: Int)(implicit ec: ExecutionContextExecutor) extends AndroidChannelFormatter(parallelism)(ec) {

  override def createPayload(message:ConnektRequest,
                           devicesInfo:Seq[DeviceDetails],
                           appDataWithId:Any):List[GCMPayloadEnvelope] = {

    val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
    val timeToLive = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)
    val dryRun = message.meta.get("x-perf-test").map(v => v.trim.equalsIgnoreCase("true"))
    val tokens = devicesInfo.map(_.token)
    val validDeviceIds = devicesInfo.map(_.deviceId)
    val payload = GCMHttpPNPayload(registration_ids = tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = Some(timeToLive), dry_run = dryRun)

    List(GCMPayloadEnvelope(message.id, message.clientId, validDeviceIds, pnInfo.appName, message.contextId.orEmpty, payload, message.meta))
  }
}
