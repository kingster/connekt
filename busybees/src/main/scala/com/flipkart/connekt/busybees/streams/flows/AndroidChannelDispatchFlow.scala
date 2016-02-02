package com.flipkart.connekt.busybees.streams.flows

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
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
class AndroidChannelDispatchFlow extends GraphStage[FlowShape[ConnektRequest, GCMPayload]] {
  val in = Inlet[ConnektRequest]("AndroidDispatchFlow.In")
  val out = Outlet[GCMPayload]("AndroidDispatchFlow.Out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val r = grab(in)

          ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelDispatchFlow:: onPush:: Received Message: ${r.getJson}")

          val pnInfo = r.channelInfo.asInstanceOf[PNRequestInfo]
          val registrationInfo = DaoFactory.getDeviceDetailsDao.get(pnInfo.appName, pnInfo.deviceId)
          val token = registrationInfo.get.token

          val appDataWithId = r.channelData.asInstanceOf[PNRequestData].data.put("messageId", r.id)
          val gcmPayload = pnInfo.platform.toUpperCase match {
            case "ANDROID" => GCMPNPayload(List[String](token), pnInfo.delayWhileIdle, appDataWithId)
            case "OPENWEB" => OpenWebGCMPayload(List[String](token))
          }

          push(out, gcmPayload)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelDispatchFlow:: onPull")
          pull(in)
        }
      })
    }
  }

  override def shape: FlowShape[ConnektRequest, GCMPayload] = FlowShape.of(in, out)
}
