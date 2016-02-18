package com.flipkart.connekt.busybees.streams.flows.formaters

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 * @author aman.shrivastava on 08/02/16.
 */

class WindowsChannelFormatter  extends GraphStage[FlowShape[ConnektRequest, WNSPNPayload]] {

  val in = Inlet[ConnektRequest]("WNSChannelFormatter.In")
  val out = Outlet[WNSPNPayload]("WNSChannelFormatter.Out")

  override def shape: FlowShape[ConnektRequest, WNSPNPayload] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter:: onPush:: Received Message: ${message.getJson}")

        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val tokens = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None).map(_.token))
        val wnsRequestPayloads = tokens.map(WNSPNPayload(_, message.channelInfo.asInstanceOf[PNRequestInfo].appName,message.channelData.asInstanceOf[PNRequestData].data.getJson.getObj[WNSTypePayload]))

        emitMultiple[WNSPNPayload](out, scala.collection.immutable.Iterable.concat(wnsRequestPayloads))

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"WindowsChannelFormatter:: onPush :: Error", e)
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
