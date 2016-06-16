package callback.stages.kafkaStage

import akka.NotUsed
import akka.stream.scaladsl.{GraphDSL, Sink}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.services.BigfootService

import scala.util.{Failure, Success}

/**
  * Created by harshit.sinha on 14/06/16.
  */
class KafkaStage() {

  def getKafkaSink(): Sink[CallbackEvent, NotUsed] = {
    Sink.fromGraph(GraphDSL.create() { implicit b =>
      val sinkDiscardedStaged = b.add(Sink.foreach(println))
      SinkShape(sinkDiscardedStaged.in)
    })
  }

}
