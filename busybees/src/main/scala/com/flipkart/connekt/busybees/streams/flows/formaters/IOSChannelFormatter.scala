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
class IOSChannelFormatter extends GraphStage[FlowShape[ConnektRequest, APSPayload]] {

  val in = Inlet[ConnektRequest]("IOSChannelFormatter.In")
  val out = Outlet[APSPayload]("IOSChannelFormatter.Out")

  override def shape: FlowShape[ConnektRequest, APSPayload] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"IOSChannelFormatter:: onPush:: Received Message: ${message.getJson}")

        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val tokens = pnInfo.deviceId.flatMap(DaoFactory.getDeviceDetailsDao.get(pnInfo.appName, _)).map(_.token)
        val iosRequestPayloads = tokens.map(iOSPNPayload(_, Map("aps" -> message.channelData.asInstanceOf[PNRequestData].data)))

        emitMultiple[APSPayload](out, immutable.Iterable.concat(iosRequestPayloads))

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"IOSChannelFormatter:: onPush :: Error", e)
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }
    })

  }

}
