package com.flipkart.connekt.busybees.streams.flows.formaters

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class AndroidChannelFormatter extends GraphStage[FlowShape[ConnektRequest, GCMPayloadEnvelope]] {

  val in = Inlet[ConnektRequest]("AndroidChannelFormatter.In")
  val out = Outlet[GCMPayloadEnvelope]("AndroidChannelFormatter.Out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in)

        ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelFormatter:: onPush:: Received Message: ${message.getJson}")

        val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
        val tokens = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None)).map(_.token)

        val appDataWithId = message.channelData.asInstanceOf[PNRequestData].data.put("messageId", message.id)
        val gcmPayload = pnInfo.platform.toUpperCase match {
          case "ANDROID" => GCMPNPayload(tokens, pnInfo.delayWhileIdle, appDataWithId)
          case "OPENWEB" => OpenWebGCMPayload(tokens)
        }

        push(out, GCMPayloadEnvelope(message.id, pnInfo.deviceId, pnInfo.appName, gcmPayload))
      }catch {
        case e:Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"AndroidChannelFormatter:: onPush :: Error", e)
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

  override def shape: FlowShape[ConnektRequest, GCMPayloadEnvelope] = FlowShape.of(in, out)

}
