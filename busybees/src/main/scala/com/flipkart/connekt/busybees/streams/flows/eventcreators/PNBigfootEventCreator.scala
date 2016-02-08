package com.flipkart.connekt.busybees.streams.flows.eventcreators

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.utils.DateTimeUtils

/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class PNBigfootEventCreator extends GraphStage[FlowShape[List[PNCallbackEvent], List[fkint.mp.connekt.PNCallbackEvent]]] {

  val in = Inlet[List[PNCallbackEvent]]("PNBigfootEventCreator.In")
  val out = Outlet[List[fkint.mp.connekt.PNCallbackEvent]]("PNBigfootEventCreator.Out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {
        val events = grab(in)
        val w: List[fkint.mp.connekt.PNCallbackEvent] = events.map(e => fkint.mp.connekt.PNCallbackEvent(messageId = e.messageId, deviceId = e.deviceId, platform = e.platform, eventType = e.eventType, appName = e.appName, contextId = e.contextId, cargo = e.cargo, timestamp = DateTimeUtils.getStandardFormatted(e.timestamp)))

        push(out, w)
      }catch {
        case e:Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"PNBigfootEventCreator:: onPush :: Error", e)
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }
    })

  }

  override def shape: FlowShape[List[PNCallbackEvent], List[fkint.mp.connekt.PNCallbackEvent]] = FlowShape.of(in, out)
}
