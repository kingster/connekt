package com.flipkart.connekt.busybees.streams.flows.formaters

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class AndroidChannelFormatter extends GraphStage[FlowShape[ConnektRequest, GCMPayload]] {

  val in = Inlet[ConnektRequest]("Android.In")
  val out = Outlet[GCMPayload]("Android.Out")


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {

        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelFormatter:: onPush:: Received Message: ${message.getJson}")

        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val registrationInfo = DaoFactory.getDeviceDetailsDao.get(pnInfo.appName, pnInfo.deviceId)
        val token: String = registrationInfo.map(_.token).orNull

        val appDataWithId = message.channelData.asInstanceOf[PNRequestData].data.put("messageId", message.id)
        val gcmPayload = pnInfo.platform.toUpperCase match {
          case "ANDROID" => GCMPNPayload(List[String](token), pnInfo.delayWhileIdle, appDataWithId)
          case "OPENWEB" => OpenWebGCMPayload(List[String](token))
        }

        push(out, gcmPayload)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }
    })

  }

  override def shape: FlowShape[ConnektRequest, GCMPayload] = FlowShape.of(in, out)

}
