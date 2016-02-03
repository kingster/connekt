package com.flipkart.connekt.busybees.streams.flows

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 2/1/16
 */
class RenderFlow extends GraphStage[FlowShape[ConnektRequest, ConnektRequest]] {

  val in = Inlet[ConnektRequest]("Render.In")
  val out = Outlet[ConnektRequest]("Render.Out")

  override def shape: FlowShape[ConnektRequest, ConnektRequest] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = try {
          val m = grab(in)

          ConnektLogger(LogFile.PROCESSORS).info(s"RenderFlow:: onPush:: Received Message: ${m.getJson}")

          lazy val cRD = m.templateId.flatMap(StencilService.get(_)).map(StencilService.render(_, m.channelDataModel)).get
          val mRendered = m.copy(channelData = Some(m.channelData).getOrElse(cRD))
          push(out, mRendered)
        } catch {
          case e: Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"RenderFlow:: onPush :: Error", e)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          ConnektLogger(LogFile.PROCESSORS).info(s"RenderFlow:: onPull")
          pull(in)
        }
      })
    }
  }
}
