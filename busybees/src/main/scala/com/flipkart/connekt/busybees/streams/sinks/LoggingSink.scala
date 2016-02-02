package com.flipkart.connekt.busybees.streams.sinks

import akka.http.scaladsl.model.HttpResponse
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{ActorMaterializer, Attributes, Inlet, SinkShape}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}

import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._
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
        message.foreach(r =>
          ConnektLogger(LogFile.WORKERS).info(s"ResponseBody: ${Await.result(r.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")),10.seconds)}")
        )
      }

    })

    override def preStart(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"LoggingSink:: preStart::")
      pull(in)
    }

  }

  override def shape: SinkShape[Try[HttpResponse]] = SinkShape(in)
}
