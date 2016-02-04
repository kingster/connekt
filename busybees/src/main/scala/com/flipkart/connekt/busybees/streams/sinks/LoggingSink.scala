package com.flipkart.connekt.busybees.streams.sinks

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpResponseString
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class LoggingSink extends GraphStage[SinkShape[Try[HttpResponseString]]] {

  val in: Inlet[Try[HttpResponseString]] = Inlet("LoggingSink.In")

  override def shape: SinkShape[Try[HttpResponseString]] = SinkShape(in)

  /* Don't kill me, I'm just trying to get this baby run */
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = BusyBeesBoot.mat

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"LoggingSink:: onPush::")
        val message = grab(in)
        message match {
          case Success(result) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"LoggingSink:: Response[${result.reponseCode}}]:: ${result.responseBody}")
          case Failure(error) =>
            ConnektLogger(LogFile.PROCESSORS).error(s"LoggingSink:: Error Recieved:: ${error.getMessage}", error)

        }
      }
    })

  }

}
