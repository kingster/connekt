/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.busybees.streams

import akka.stream._
import akka.stream.stage.{OutHandler, GraphStage, GraphStageLogic, InHandler}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.util.control.NonFatal

trait SupervisedGraphStage

abstract class SupervisedFlowStage[In, Out](stageName: String = "")(implicit val strategy: Supervision.Decider = Supervision.stoppingDecider) extends GraphStage[FlowShape[In, Out]] with SupervisedGraphStage {
  def processInput(inputMessage: In): List[Out]

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(i, new InHandler {
      override def onPush(): Unit = try {
        val inMessage = grab(i)
        val outMessages = processInput(inMessage)

        if(isAvailable(o))
          emitMultiple[Out](o, outMessages.iterator, () => {
            ConnektLogger(LogFile.PROCESSORS).trace(s"${stageName}_SupervisedFlowStage pushed downstream for input: $inMessage.")
          })

      } catch {
        case NonFatal(e) => strategy(e) match {
          case Supervision.Stop =>
            failStage(e)
          case Supervision.Resume =>
            if(!hasBeenPulled(i))
              pull(i)
          case Supervision.Restart =>
            ConnektLogger(LogFile.PROCESSORS).error(s"${stageName}_Restart strategy not supported for SupervisedFlowStage.")
            throw new RuntimeException("Restart strategy not supported for SupervisedFlowStage.")
        }
      }
    })

    setHandler(o, new OutHandler {
      override def onPull(): Unit =
        if(!hasBeenPulled(i)) {
          pull(i)
          ConnektLogger(LogFile.PROCESSORS).trace(s"${stageName}_SupervisedFlowStage pulled upstream on downstream pull.")
        }
    })
  }

  val i = Inlet[In](s"$stageName.in")
  val o = Outlet[Out](s"$stageName.out")

  override def shape: FlowShape[In, Out] = FlowShape.of(i, o)
}
