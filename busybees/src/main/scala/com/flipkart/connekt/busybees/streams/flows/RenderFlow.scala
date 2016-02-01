package com.flipkart.connekt.busybees.streams.flows

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.StencilService

/**
 *
 *
 * @author durga.s
 * @version 2/1/16
 */
class RenderFlow extends GraphStage[FlowShape[ConnektRequest, ConnektRequest]]{

  val in = Inlet[ConnektRequest]("Render.In")
  val out = Outlet[ConnektRequest]("Render.Out")

  override def shape: FlowShape[ConnektRequest, ConnektRequest] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val m = grab(in)
          lazy val cRD = m.templateId.flatMap(StencilService.get(_)).map(StencilService.render(_, m.channelDataModel)).get
          val mRendered = m.copy(channelData = Some(m.channelData).getOrElse(cRD))
          push(out, mRendered)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
    }
  }
}
