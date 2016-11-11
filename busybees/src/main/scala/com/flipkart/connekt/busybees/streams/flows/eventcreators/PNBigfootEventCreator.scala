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
package com.flipkart.connekt.busybees.streams.flows.eventcreators

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent

class PNBigfootEventCreator extends GraphStage[FlowShape[PNCallbackEvent, fkint.mp.connekt.PNCallbackEvent]] {

  val in = Inlet[PNCallbackEvent]("PNBigfootEventCreator.In")
  val out = Outlet[fkint.mp.connekt.PNCallbackEvent]("PNBigfootEventCreator.Out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val event = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"PNBigfootEventCreator received event: ${event.messageId}")
        try {
          if(isAvailable(out))
            push(out, event.toPublishFormat)
        }catch {
          case e:Throwable =>
            ConnektLogger(LogFile.PROCESSORS).error(s"PNBigfootEventCreator error", e)
            if(!hasBeenPulled(in)) {
              pull(in)
              ConnektLogger(LogFile.PROCESSORS).trace(s"PNBigfootEventCreator pulled upstream for: ${event.messageId}")
            }
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if(!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).trace(s"PNBigfootEventCreator:: PULLED upstream on downstream pull.")
        }
      }
    })

  }

  override def shape  = FlowShape.of(in, out)
}
