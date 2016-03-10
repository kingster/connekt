package com.flipkart.connekt.busybees.streams.flows

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.metrics.Instrumented
import fkint.mp.connekt.PNCallbackEvent

/**
 * Created by kinshuk.bairagi on 09/03/16.
 */
class FlowMetrics(category: String) extends GraphStage[FlowShape[PNCallbackEvent, PNCallbackEvent]] with Instrumented {

  val in = Inlet[PNCallbackEvent]("FlowMetrics.In")
  val out = Outlet[PNCallbackEvent]("FlowMetrics.Out")

  override def shape: FlowShape[PNCallbackEvent, PNCallbackEvent] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val event = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"FlowMetrics:: ON_PUSH for ${event.messageId}")
        try {

          meter("flow.context." + event.contextId).mark()
          meter("flow.os." + event.platform).mark()
          meter("flow.et." + event.eventType).mark()
          meter(s"flow.total.$category").mark()

          if (isAvailable(out)) {
            ConnektLogger(LogFile.PROCESSORS).debug(s"FlowMetrics:: PUSHED downstream for ${event.messageId}")
            push(out, event)
          }
        } catch {
          case e: Exception =>
            ConnektLogger(LogFile.PROCESSORS).error(s"FlowMetrics:: Error ${event.messageId}", e)
            if (!hasBeenPulled(in)) {
              pull(in)
              ConnektLogger(LogFile.PROCESSORS).debug(s"FlowMetrics:: PULLED upstream for ${event.messageId}")
            }
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).info(s"FlowMetrics:: PULLED upstream on downstream pull.")
        }
      }
    })

  }

}
