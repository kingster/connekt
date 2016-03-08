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
      override def onPush(): Unit = {
        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"AndroidChannelFormatter:: ON_PUSH for ${message.id}")
        try {
          ConnektLogger(LogFile.PROCESSORS).info(s"AndroidChannelFormatter:: onPush:: Received Message: ${message.getJson}")

          val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]
          val tokens = pnInfo.deviceId.flatMap(DeviceDetailsService.get(pnInfo.appName, _).getOrElse(None)).map(_.token)

          val appDataWithId = message.channelData.asInstanceOf[PNRequestData].data.put("messageId", message.id)
          val dryRun = message.meta.get("x-perf-test").map(v => v.trim.equalsIgnoreCase("true"))
          val gcmPayload = pnInfo.platform.toUpperCase match {
            case "ANDROID" => GCMPNPayload(tokens, delay_while_idle = Option(pnInfo.delayWhileIdle), appDataWithId, time_to_live = None, dry_run = dryRun)
            case "OPENWEB" => OpenWebGCMPayload(tokens, dry_run = None)
          }

          if (tokens.nonEmpty && isAvailable(out)) {
            push(out, GCMPayloadEnvelope(message.id, pnInfo.deviceId, pnInfo.appName, gcmPayload))
            ConnektLogger(LogFile.PROCESSORS).debug(s"AndroidChannelFormatter:: PUSHED downstream for ${message.id}")
          } else {
            ConnektLogger(LogFile.PROCESSORS).warn(s"AndroidChannelFormatter:: No Device Details found for : ${pnInfo.deviceId}, msgId: ${message.id}")
          }

        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"AndroidChannelFormatter:: onPush :: Error", e)
        } finally {
          if (!hasBeenPulled(in)) {
            pull(in)
            ConnektLogger(LogFile.PROCESSORS).debug(s"AndroidChannelFormatter:: PULLED upstream for ${message.id}")
          }
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).debug(s"AndroidChannelFormatter:: PULLED upstream on downstream pull.")
        }
      }
    })

  }

  override def shape: FlowShape[ConnektRequest, GCMPayloadEnvelope] = FlowShape.of(in, out)

}
