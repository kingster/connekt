/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.busybees.streams.flows

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.metrics.Instrumented
import fkint.mp.connekt.PNCallbackEvent

class FlowMetrics[T](names: String*) extends GraphStage[FlowShape[T, T]] with Instrumented {

  val in = Inlet[T]("FlowMetrics.In")
  val out = Outlet[T]("FlowMetrics.Out")

  override def shape: FlowShape[T, T] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val event = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"FlowMetrics:: ON_PUSH")
        try {

          names.foreach(name => meter(s"flow.stage.$name").mark())

          if (isAvailable(out)) {
            ConnektLogger(LogFile.PROCESSORS).debug(s"FlowMetrics:: PUSHED downstream")
            push(out, event)
          }
        } catch {
          case e: Exception =>
            ConnektLogger(LogFile.PROCESSORS).error(s"FlowMetrics:: Error ", e)
            if (!hasBeenPulled(in)) {
              pull(in)
              ConnektLogger(LogFile.PROCESSORS).debug(s"FlowMetrics:: PULLED upstream for")
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
