package com.flipkart.connekt.busybees.streams.flows.formaters

import akka.stream.stage.{InHandler, OutHandler, GraphStage, GraphStageLogic}
import akka.stream.{Outlet, Inlet, Attributes, FlowShape}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
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
      override def onPush(): Unit = try {

        ConnektLogger(LogFile.PROCESSORS).debug(s"IOSChannelFormatter:: onPush.")
        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"IOSChannelFormatter:: Received Message: ${message.getJson}")
        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val tokens = pnInfo.deviceId.flatMap(DaoFactory.getDeviceDetailsDao.get(pnInfo.appName, _)).map(_.token)
        val apnsPayloads = tokens.map(iOSPNPayload(_, Map("aps" -> message.channelData.asInstanceOf[PNRequestData].data)))
        val apnsEnvelopes = apnsPayloads.map(APSPayloadEnvelope(message.id, pnInfo.deviceId, pnInfo.appName, _))

        if(apnsEnvelopes.nonEmpty)
          emitMultiple[APSPayloadEnvelope](out, apnsEnvelopes.iterator)
        else if(!hasBeenPulled(in))
          pull(in)

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"IOSChannelFormatter:: onPush :: Error", e)
          if(!hasBeenPulled(in))
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if(!hasBeenPulled(in))
          pull(in)
      }
    })

  }

}
