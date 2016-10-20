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
package com.flipkart.connekt.busybees.streams.sinks

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.BigfootService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.{Failure, Success}

class EventSenderSink extends GraphStage[SinkShape[fkint.mp.connekt.PNCallbackEvent]] {

  val in: Inlet[fkint.mp.connekt.PNCallbackEvent] = Inlet("EventSenderSink.In")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val event = grab(in)
//        ConnektLogger(LogFile.PROCESSORS).debug(s"EventSenderSink on push for: ${event.messageId}")
        BigfootService.ingestEvent(event, "") match {
          case Success(true) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"EventSenderSink ${event.messageId} | SUCCESS")
          case Success(false) =>
            ConnektLogger(LogFile.PROCESSORS).error(s"EventSenderSink event ingestion failed for: ${event.getJson}")
          case Failure(t) =>
            ConnektLogger(LogFile.PROCESSORS).error(s"EventSenderSink event ingestion failed for: ${event.getJson}", t)
        }
      }
    })
  }

  override def shape  = SinkShape(in)
}
