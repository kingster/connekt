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
package com.flipkart.connekt.busybees.streams.flows

import akka.stream._
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent

import scala.concurrent.Future
import scala.util.control.NonFatal

private [busybees] abstract class MapFlowStage[In, Out] {

  protected val stageName: String = this.getClass.getSimpleName

  val map: In => List[Out]

  def flow = Flow[In].mapConcat(map).named(stageName)
}

private [busybees] abstract class MapAsyncFlowStage[In, Out](parallelism:Int = 128) {

  protected val stageName: String = this.getClass.getSimpleName

  val map: In => Future[List[Out]]

  def flow = Flow[In].mapAsyncUnordered(parallelism)(map).mapConcat(identity).named(stageName)
}

private [busybees] abstract class MapGraphStage[In, Out] extends GraphStage[FlowShape[In, Out]] {

  protected val stageName: String = this.getClass.getSimpleName

  val map: In => List[Out]

  val i = Inlet[In](s"$stageName.in")
  val o = Outlet[Out](s"$stageName.out")

  override def shape: FlowShape[In, Out] = FlowShape.of(i, o)

  class SupervisedGraphStageLogic extends GraphStageLogic(shape) {

    setHandler(i, new InHandler {
      override def onPush(): Unit = try {
        val inMessage = grab(i)
        val outMessages = map(inMessage)

        if(isAvailable(o))
          emitMultiple[Out](o, outMessages.iterator, () => {
            ConnektLogger(LogFile.PROCESSORS).trace(s"${stageName}_SupervisedFlowStage pushed downstream for input: $inMessage.")
          })

      } catch {
        case NonFatal(e) => StageSupervision.decider(e) match {
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
        }
    })
  }

  override def createLogic(inheritedAttributes: Attributes) = new SupervisedGraphStageLogic
}

object StageSupervision {
  val decider: Supervision.Decider = {
    case cEx: ConnektPNStageException =>
      ConnektLogger(LogFile.PROCESSORS).warn("StageSupervision Handle ConnektPNStageException")
      cEx.deviceId
        .map(PNCallbackEvent(cEx.messageId, _, cEx.eventType, cEx.platform, cEx.appName, cEx.context, cEx.getMessage, cEx.timeStamp))
        .persist
      Supervision.Resume

    case e:Throwable =>
      ConnektLogger(LogFile.PROCESSORS).error("StageSupervision Handle Unknown Exception",e)
      Supervision.Stop
  }
}
