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

class WindowsChannelFormatter  extends GraphStage[FlowShape[ConnektRequest, WNSPayloadEnvelope]] {

  val in = Inlet[ConnektRequest]("WNSChannelFormatter.In")
  val out = Outlet[WNSPayloadEnvelope]("WNSChannelFormatter.Out")

  override def shape: FlowShape[ConnektRequest, WNSPayloadEnvelope] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter:: onPush:: Received Message: ${message.getJson}")

        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val tokens = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None).map(_.token))
        val wnsRequestPayloads = tokens.map(WNSPayloadEnvelope(message.id, _, message.channelInfo.asInstanceOf[PNRequestInfo].appName, message.channelData.asInstanceOf[PNRequestData].data.getJson.getObj[WNSPayload]))

//        push(out, wnsRequestPayloads.head)
        emitMultiple[WNSPayloadEnvelope](out, scala.collection.immutable.Iterable.concat(wnsRequestPayloads))

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"WindowsChannelFormatter:: onPush :: Error", e)
          pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info("WindowsChannelFormatter:: onUpstream finish invoked")
        super.onUpstreamFinish()
      }

      override def onUpstreamFailure(e: Throwable): Unit = {
        ConnektLogger(LogFile.PROCESSORS).error(s"WindowsChannelFormatter:: onUpstream failure: ${e.getMessage}", e)
        super.onUpstreamFinish()
      }
    })


    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }

      override def onDownstreamFinish(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info("WindowsChannelFormatter:: onDownstreamFinish finish invoked")
        super.onDownstreamFinish()
      }
    })

  }

}
