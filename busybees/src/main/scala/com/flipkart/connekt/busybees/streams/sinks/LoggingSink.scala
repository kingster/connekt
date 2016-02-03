package com.flipkart.connekt.busybees.streams.sinks

import akka.http.scaladsl.model.HttpResponse
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import akka.util.{ByteString, ByteStringBuilder}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.util.{Failure, Success, Try}
/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
class LoggingSink extends GraphStage[SinkShape[Try[HttpResponse]]] {

  val in: Inlet[Try[HttpResponse]] = Inlet("LoggingSink.In")

  /* Don't kill me, I'm just trying to get this baby run */
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = BusyBeesBoot.mat

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"LoggingSink:: onPush::")
        val message = grab(in)
        message.foreach(r => {
          r.entity.dataBytes.runFold[ByteStringBuilder](ByteString.newBuilder)((u, bs) => {u ++= bs}).onComplete {
            case Success(b) =>
              ConnektLogger(LogFile.PROCESSORS).info(s"LoggingSink:: ResponseBody:: ${b.result().decodeString("UTF-8")}")
            case Failure(t) =>
              ConnektLogger(LogFile.PROCESSORS).error(s"LoggingSink:: Error Processing ResponseBody:: ${t.getMessage}", t)
          }
        })
      }
    })

    override def preStart(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"LoggingSink:: preStart::")
      pull(in)
    }

  }

  override def shape: SinkShape[Try[HttpResponse]] = SinkShape(in)
}
