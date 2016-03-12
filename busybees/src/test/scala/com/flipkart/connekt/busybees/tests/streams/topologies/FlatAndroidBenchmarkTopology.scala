package com.flipkart.connekt.busybees.tests.streams.topologies

import java.util.concurrent.atomic.AtomicLong

import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import akka.stream.{Attributes, FlowShape}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{GCMDispatcherPrepare, HttpDispatcher}
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters.AndroidChannelFormatter
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.GCMResponseHandler
import com.flipkart.connekt.busybees.streams.flows.{FlowMetrics, RenderFlow}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNCallbackEvent}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.factories.ConnektLogger
import com.flipkart.connekt.commons.utils.StringUtils._
import org.scalatest.Ignore

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}


/**
 *
 *
 * @author durga.s
 * @version 3/11/16
 */
@Ignore
class FlatAndroidBenchmarkTopology extends TopologyUTSpec {

  override def beforeAll() = {
    super.beforeAll()
    HttpDispatcher.init(ConnektConfig.getConfig("busybees.akka.http").get)
  }

  val ioDispatcher = system.dispatchers.lookup("akka.stream.default-blocking-io-dispatcher")
  val fmtAndroidParallelism = ConnektConfig.getInt("busybees.topology.push.androidFormatter.parallelism").get

  "FlatAndroidBenchmarkTopology with responseHandler" should "log throughput rates" in {
    val counter: AtomicLong = new AtomicLong(0)

    val topic = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.head
    val kSource = Source.fromGraph(new KafkaSource[ConnektRequest](getKafkaConsumerHelper, topic, 5)(Promise[String]().future))

    val render = Flow.fromGraph(new RenderFlow)
    val gcmFmt = Flow.fromGraph(new AndroidChannelFormatter(fmtAndroidParallelism)(ioDispatcher).flow)
    val gcmPrepare = Flow.fromGraph(new GCMDispatcherPrepare)
    val gcmRHandler = Flow.fromGraph(new GCMResponseHandler)
    val meter = Flow.fromGraph(new FlowMetrics[fkint.mp.connekt.PNCallbackEvent](Channel.PUSH))
    val eventCreator = Flow.fromGraph(new PNBigfootEventCreator)

    val gWithRHandler = kSource.via(render).via(gcmFmt).via(gcmPrepare).withAttributes(Attributes.asyncBoundary).via(HttpDispatcher.gcmPoolClientFlow).via(gcmRHandler).to(Sink.foreach(e => {
      if(0 == (counter.incrementAndGet() % 1000))
        println(s"FlatAndroidBenchmarkTopology:: Processed ${counter.get()} messages by ${System.currentTimeMillis()}")
    }))

    gWithRHandler.run()

    Await.result(Promise[String]().future, 400.seconds)
  }

  "FlatAndroidBenchmarkTopology without responseHandler" should "log throughput rates" in {
    val counter = new AtomicLong(0)

    val topic = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.head
    val kSource = Source.fromGraph(new KafkaSource[ConnektRequest](getKafkaConsumerHelper, topic, 5)(Promise[String]().future))

    val render = Flow.fromGraph(new RenderFlow)
    val gcmFmt = Flow.fromGraph(new AndroidChannelFormatter(fmtAndroidParallelism)(ioDispatcher).flow)
    val gcmPrepare = Flow.fromGraph(new GCMDispatcherPrepare)
    val gcmRHandler = Flow.fromGraph(new GCMResponseHandler)
    val meter = Flow.fromGraph(new FlowMetrics[fkint.mp.connekt.PNCallbackEvent](Channel.PUSH))
    val eventCreator = Flow.fromGraph(new PNBigfootEventCreator)

    val gWithoutRHandler = kSource.via(render).via(gcmFmt).via(gcmPrepare).withAttributes(Attributes.asyncBoundary).via(HttpDispatcher.gcmPoolClientFlow).map(rT => {
      rT._1.foreach(_.entity.getString.getObj[ObjectNode])
      if(0 == (counter.incrementAndGet() % 1000))
        ConnektLogger(LogFile.PROCESSORS).info(s"Processed ${counter.get()} messages by ${System.currentTimeMillis()}")

      PNCallbackEvent("", "", "", "", "", "", "", 0)
    }).via(eventCreator).via(meter).to(Sink.ignore)

    gWithoutRHandler.run()

    Await.result(Promise[String]().future, 400.seconds)
  }

  "FlatAndroidBenchmarkTopology created with GraphDSL builder" should "log throughput rates" in {
    val counter: AtomicLong = new AtomicLong(0)
    val prevTime = new AtomicLong(System.currentTimeMillis())

    val topic = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.head
    val kSource = Source.fromGraph(new KafkaSource[ConnektRequest](getKafkaConsumerHelper, topic, 5)(Promise[String]().future))

    val render = Flow.fromGraph(new RenderFlow)
    val gcmFmt = Flow.fromGraph(new AndroidChannelFormatter(fmtAndroidParallelism)(ioDispatcher).flow)
    val gcmPrepare = Flow.fromGraph(new GCMDispatcherPrepare)
    val gcmRHandler = Flow.fromGraph(new GCMResponseHandler)
    val meter = Flow.fromGraph(new FlowMetrics[fkint.mp.connekt.PNCallbackEvent](Channel.PUSH))
    val eventCreator = Flow.fromGraph(new PNBigfootEventCreator)

    val vanillaSource = Source.fromGraph(kSource)

    val complexFlow = Flow.fromGraph(GraphDSL.create(){implicit b =>
      val start = b.add(render)
      val end = b.add(meter)

      start ~> b.add(gcmFmt) ~> b.add(gcmPrepare) ~> b.add(HttpDispatcher.gcmPoolClientFlow.map(rT => {
        rT._1.foreach(_.entity.getString.getObj[ObjectNode])
        if (0 == (counter.incrementAndGet() % 1000)) {
          val currentTime = System.currentTimeMillis()
          val rate = 1000000/(currentTime - prevTime.getAndSet(currentTime))
          println(s"FlatAndroidBenchmarkTopology #Rate: [$rate] upto ${counter.get()} messages by $currentTime")
        }

        rT
      })) ~> b.add(gcmRHandler) ~> b.add(eventCreator) ~> end


      FlowShape(start.in, end.out)
    })

    val vanillaSink = Sink.fromGraph(Sink.ignore)

    val g = vanillaSource.via(complexFlow).runWith(vanillaSink)

    Await.result(g, 600.seconds)
  }
}
