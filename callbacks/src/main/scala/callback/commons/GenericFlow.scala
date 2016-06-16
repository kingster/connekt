package callback.commons

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

/**
  * Created by harshit.sinha on 03/06/16.
  * just a template class to make any flow of type input A and output B
  */

class GenericFlow[A, B](f: A => B) extends GraphStage[FlowShape[A, B]] {

  val in = Inlet[A]("Inlet.A")
  val out = Outlet[B]("Outlet.B")

  override val shape = FlowShape.of(in, out)

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val value = grab(in)
          push(out, f(value))
        }
      })
      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          if (!hasBeenPulled(in))
            pull(in)
        }
      })
    }
}


