package com.flipkart.connekt.busybees.streams.flows.formaters

import java.util.concurrent.TimeUnit

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class IOSChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, APSPayloadEnvelope](parallelism)(ec) {

  def getExpiry(ts: Option[Long]): Long = {
    ts.getOrElse(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6))
  }

  override def map: (ConnektRequest) => List[APSPayloadEnvelope] = message => {

    try {

      ConnektLogger(LogFile.PROCESSORS).info(s"IOSChannelFormatter:: Received Message: ${message.getJson}")
      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
      val listOfTokenDeviceId = pnInfo.deviceId.flatMap(DaoFactory.getDeviceDetailsDao.get(pnInfo.appName, _)).map(r => (r.token, r.deviceId))
      val apnsEnvelopes = listOfTokenDeviceId.map(td => {
        val apnsPayload = iOSPNPayload(td._1, getExpiry(message.expiryTs), Map("aps" -> message.channelData.asInstanceOf[PNRequestData].data))
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
