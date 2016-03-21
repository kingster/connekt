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

import java.util.concurrent.TimeUnit

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.{PNStencilService, StencilService, DeviceDetailsService}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor

class IOSChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, APSPayloadEnvelope](parallelism)(ec) {

  def getExpiry(ts: Option[Long]): Long = {
    ts.getOrElse(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6))
  }

  override def map: (ConnektRequest) => List[APSPayloadEnvelope] = message => {

    try {

      ConnektLogger(LogFile.PROCESSORS).info(s"IOSChannelFormatter:: Received Message: ${message.getJson}")
      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
      val listOfTokenDeviceId = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None)).map(r => (r.token, r.deviceId))
      val iosStencil = StencilService.get(s"ckt-${pnInfo.appName}-ios").get

      val apnsEnvelopes = listOfTokenDeviceId.map(td => {
        val apsData = PNStencilService.getPNData(iosStencil, message.channelData.asInstanceOf[PNRequestData].data).getObj[ObjectNode]
        val apnsPayload = iOSPNPayload(td._1, getExpiry(message.expiryTs), Map("aps" -> apsData))
        APSPayloadEnvelope(message.id, td._2, pnInfo.appName, apnsPayload)
      })

      if (apnsEnvelopes.nonEmpty) {
        val dryRun = message.meta.get("x-perf-test").exists(_.trim.equalsIgnoreCase("true"))
        if (!dryRun) {
          ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter:: PUSHED downstream for ${message.id}")
          apnsEnvelopes
        }
        else {
          ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter:: Dry Run Dropping msgId: ${message.id}")
          List.empty[APSPayloadEnvelope]
        }
      } else {
        ConnektLogger(LogFile.PROCESSORS).warn(s"IOSChannelFormatter:: No Device Details found for : ${pnInfo.deviceId}, msgId: ${message.id}")
        List.empty[APSPayloadEnvelope]
      }

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"IOSChannelFormatter:: OnFormat error", e)
        List.empty[APSPayloadEnvelope]
    }
  }
}
