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

  val in: Inlet[Try[HttpResponse]] = Inlet("KafkaMessageSource.Out")

  /* Don't kill me, I'm just trying to get this baby run */
  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = ActorMaterializer()

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        grab(in).foreach(r =>
          ConnektLogger(LogFile.WORKERS).info(s"ResponseBody: ${Await.result(r.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")),10.seconds)}")
        )
      }
    })
  }

  override def shape: SinkShape[Try[HttpResponse]] = SinkShape(in)
}
