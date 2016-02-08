package com.flipkart.connekt.busybees.streams.sinks

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.BigfootService
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.{Failure, Success}
/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class EventSenderSink extends GraphStage[SinkShape[List[fkint.mp.connekt.PNCallbackEvent]]] {
  val in: Inlet[List[fkint.mp.connekt.PNCallbackEvent]] = Inlet("EventSenderSink.In")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"EventSenderSink:: onPush::")
        val events = grab(in)
        events.foreach(e =>
          BigfootService.ingest(e) match {
            case Success(true) =>
              ConnektLogger(LogFile.PROCESSORS).info(s"EventSenderSink:: ${e.messageId} | SUCCESS")
            case Failure(t) =>
              ConnektLogger(LogFile.PROCESSORS).error(s"EventSenderSink:: Event: [${e.getJson}] ingestion failed. ${t.getMessage}")
          })
      }
    })

  }

  override def shape: SinkShape[List[fkint.mp.connekt.PNCallbackEvent]] = SinkShape(in)
}
