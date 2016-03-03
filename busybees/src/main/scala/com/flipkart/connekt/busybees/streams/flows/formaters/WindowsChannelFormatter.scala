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

class WindowsChannelFormatter extends GraphStage[FlowShape[ConnektRequest, WNSPayloadEnvelope]] {

  val in = Inlet[ConnektRequest]("WNSChannelFormatter.In")
  val out = Outlet[WNSPayloadEnvelope]("WNSChannelFormatter.Out")

  override def shape: FlowShape[ConnektRequest, WNSPayloadEnvelope] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter:: onPush:: Received Message: ${message.getJson}")

        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val wnsPayload = message.channelData.asInstanceOf[PNRequestData].data.getJson.getObj[WNSPayload]
        val devices = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None))
        val wnsRequestEnvelopes = devices.map(d => WNSPayloadEnvelope(message.id, d.token, message.channelInfo.asInstanceOf[PNRequestInfo].appName, d.deviceId, wnsPayload))

        if (wnsRequestEnvelopes.nonEmpty)
          emitMultiple[WNSPayloadEnvelope](out, wnsRequestEnvelopes.iterator, () => {
              ConnektLogger(LogFile.PROCESSORS).info(s"WindowsChannelFormatter:: emitMultiple :: Completed")
          })
        else if (!hasBeenPulled(in))
          pull(in)

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"WindowsChannelFormatter:: onPush :: Error", e)
          if (!hasBeenPulled(in))
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
        if (!hasBeenPulled(in))
          pull(in)
      }

      override def onDownstreamFinish(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info("WindowsChannelFormatter:: onDownstreamFinish finish invoked")
        super.onDownstreamFinish()
      }
    })

  }

}
