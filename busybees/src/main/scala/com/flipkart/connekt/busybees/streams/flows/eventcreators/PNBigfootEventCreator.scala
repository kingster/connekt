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
class PNBigfootEventCreator extends GraphStage[FlowShape[PNCallbackEvent, fkint.mp.connekt.PNCallbackEvent]] {

  val in = Inlet[PNCallbackEvent]("PNBigfootEventCreator.In")
  val out = Outlet[fkint.mp.connekt.PNCallbackEvent]("PNBigfootEventCreator.Out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {
        val event = grab(in)
        push(out, event.toBigfootFormat)
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

  override def shape  = FlowShape.of(in, out)
}
