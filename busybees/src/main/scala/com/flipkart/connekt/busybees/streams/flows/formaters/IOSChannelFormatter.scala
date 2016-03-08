package com.flipkart.connekt.busybees.streams.flows.formaters

import java.util.concurrent.TimeUnit

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.immutable

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class IOSChannelFormatter extends GraphStage[FlowShape[ConnektRequest, APSPayloadEnvelope]] {

  val in = Inlet[ConnektRequest]("IOSChannelFormatter.In")
  val out = Outlet[APSPayloadEnvelope]("IOSChannelFormatter.Out")

  override def shape: FlowShape[ConnektRequest, APSPayloadEnvelope] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {

        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter:: ON_PUSH for ${message.id}")

        try {
          ConnektLogger(LogFile.PROCESSORS).info(s"IOSChannelFormatter:: Received Message: ${message.getJson}")
          val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
          val listOfTokenDeviceId = pnInfo.deviceId.flatMap(DaoFactory.getDeviceDetailsDao.get(pnInfo.appName, _)).map(r => (r.token, r.deviceId))
          val apnsEnvelopes = listOfTokenDeviceId.map(td => {
            val apnsPayload = iOSPNPayload(td._1, getExpiry(message.expiryTs), Map("aps" -> message.channelData.asInstanceOf[PNRequestData].data))
            APSPayloadEnvelope(message.id, td._2, pnInfo.appName, apnsPayload)
          })

          if (apnsEnvelopes.nonEmpty)
            emitMultiple[APSPayloadEnvelope](out, apnsEnvelopes.iterator, () => {
              ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter:: PUSHED downstream for ${message.id}")
            })
          else
            ConnektLogger(LogFile.PROCESSORS).warn(s"IOSChannelFormatter:: No Device Details found for : ${pnInfo.deviceId}, msgId: ${message.id}")

        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"IOSChannelFormatter:: onPush :: Error", e)
        } finally {
          if (!hasBeenPulled(in)) {
            pull(in)
            ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter:: PULLED upstream for ${message.id}")
          }
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in))
          pull(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter:: PULLED upstream on downstream pull.")
      }
    })

  }

  private def getExpiry(ts: Option[Long]): Long = {
    ts.getOrElse(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6))
  }

}
