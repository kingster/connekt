package com.flipkart.connekt.busybees.streams.flows.formaters

import akka.stream.stage.{InHandler, OutHandler, GraphStage, GraphStageLogic}
import akka.stream.{Outlet, Inlet, Attributes, FlowShape}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class IOSChannelFormatter extends GraphStage[FlowShape[ConnektRequest, APSPayload]] {

  val in = Inlet[ConnektRequest]("iOS.In")
  val out = Outlet[APSPayload]("iOS.Out")

  override def shape: FlowShape[ConnektRequest, APSPayload] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"IOSChannelFormatter:: onPush:: Received Message: ${message.getJson}")

        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val registrationInfo = DaoFactory.getDeviceDetailsDao.get(pnInfo.appName, pnInfo.deviceId)

        //TODO : Handle null values.
        val token: String = registrationInfo.map(_.token).orNull

        val iosPayload = iOSPNPayload(token, Map("aps" -> message.channelData.asInstanceOf[PNRequestData].data))

        push(out, iosPayload)

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
