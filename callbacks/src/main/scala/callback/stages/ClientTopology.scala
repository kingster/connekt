package callback.stages

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{GraphDSL, Partition, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, Graph}
import callback.stages.httpStage.HTTPStage
import callback.stages.kafkaStage.KafkaStage
import callback.topologyManager.ClientTopologyManager
import com.flipkart.connekt.busybees.streams.sources.{KafkaSource, CallbackKafkaSource}
import com.flipkart.connekt.commons.entities.fabric.FabricMaker
import com.flipkart.connekt.commons.entities.{Evaluator, HTTPEndpoint, KafkaEndpoint, Subscription}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.services.ConnektConfig
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.Try

/**
  * Created by harshit.sinha on 03/06/16.
  */

class ClientTopology(subscriptionRequest: Subscription, topologyManager: ClientTopologyManager)(implicit am: ActorMaterializer,sys: ActorSystem, ec: ExecutionContext) {


  var subscription = subscriptionRequest
  var myEvaluator = FabricMaker.create[Evaluator](subscription.sId, subscription.groovyString)
  var promised:Promise[String] = _
  var kafkaCallbackSource: CallbackKafkaSource[CallbackEvent]  = _
  // var kafkaCallbackSource: KafkaSource[CallbackEvent]  = _
  var graph : Graph[ClosedShape.type, NotUsed] = _
  var currentShutdown = 0
  val kafkaConsumerConnConf = ConnektConfig.getConfig("connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())
  val kafkaConsumerPoolConf = ConnektConfig.getConfig("connections.kafka.consumerPool").getOrElse(ConfigFactory.empty())
  //val kafkaConsumerHelper = KafkaConsumerHelper(kafkaConsumerConnConf, kafkaConsumerPoolConf)
  val clientTopologyManager = topologyManager
  var active: Boolean =_
  val retryLimit = 4
  var sinkStage : Sink[CallbackEvent,NotUsed] =_

  def onStart(): Unit = {

    promised = Promise[String]()
   /* if(promised.isCompleted)
      println("complter")
    else
    println("not completer")*/
    kafkaCallbackSource = new CallbackKafkaSource[CallbackEvent](subscription.sId, "active_events", kafkaConsumerConnConf)(promised.future)
    //kafkaCallbackSource = new KafkaSource[CallbackEvent](kafkaConsumerHelper, "active_events")(promised.future)

    val source = Source.fromGraph(kafkaCallbackSource).filter(myEvaluator.evaluate)



    subscription.endpoint match {
      case http: HTTPEndpoint => sinkStage = new HTTPStage(this).getHttpSink()
      case kafka: KafkaEndpoint => sinkStage = new KafkaStage().getKafkaSink()
    }

    graph = GraphDSL.create() {
      implicit b =>

        val sourceStaged = b.add(source)
        val sinkStaged = b.add(sinkStage)

        sourceStaged ~> sinkStaged
        ClosedShape
    }
    RunnableGraph.fromGraph(graph).run()
    println("rerun"+ System.currentTimeMillis())
    active = true
  }

  def onStop(): Unit = {
    promised.success("User Signal clientTopology to shutdown")
    active = false
  }

}
