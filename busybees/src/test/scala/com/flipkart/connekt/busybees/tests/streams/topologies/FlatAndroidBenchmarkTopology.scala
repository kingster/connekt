package com.flipkart.connekt.busybees.tests.streams.topologies

import java.util.concurrent.atomic.AtomicLong

import akka.stream.SourceShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{GCMDispatcherPrepare, HttpDispatcher}
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters.AndroidChannelFormatter
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.GCMResponseHandler
import com.flipkart.connekt.busybees.streams.flows.{FlowMetrics, RenderFlow}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

/**
 *
 *
 * @author durga.s
 * @version 3/11/16
 */
//@Ignore
class FlatAndroidBenchmarkTopology extends TopologyUTSpec with Instrumented {

  override def beforeAll() = {
    super.beforeAll()
    HttpDispatcher.init(ConnektConfig.getConfig("react").get)
  }

  lazy val ioDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")
  lazy val fmtAndroidParallelism = ConnektConfig.getInt("busybees.topology.push.androidFormatter.parallelism").get

  "FlatAndroidBenchmarkTopology with responseHandler" should "log throughput rates" in {
    val counter: AtomicLong = new AtomicLong(0)
    val prevTime = new AtomicLong(System.currentTimeMillis())

    val topic = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get.head
    val kSource = Source.fromGraph(new KafkaSource[ConnektRequest](getKafkaConsumerHelper, topic, 10)(Promise[String]().future))
    val repeatSource = Source.repeat {
      """
        |{
        |	"channel": "PN",
        |	"sla": "H",
        |	"channelData": {
        |		"type": "PN",
        |		"data": {
        |			"message": "Hello Kinshuk. GoodLuck!",
        |			"title": "Kinshuk GCM Push Test",
        |			"id": "123456789",
        |			"triggerSound": true,
        |			"notificationType": "Text"
        |
        |		}
        |	},
        |	"channelInfo" : {
        |	    "type" : "PN",
        |	    "ackRequired": true,
        |    	"delayWhileIdle": true,
        |     "platform" :  "android",
        |     "appName" : "ConnectSampleApp",
        |     "deviceId" : ["513803e45cf1b344ef494a04c9fb650a"]
        |	},
        |	"meta": {
        |   "x-perf-test" : "true"
        | }
        |}
      """.stripMargin.getObj[ConnektRequest]
    }

    val render = Flow.fromGraph(new RenderFlow)
    val gcmFmt = Flow.fromGraph(new AndroidChannelFormatter(fmtAndroidParallelism)(ioDispatcher).flow)
    val gcmPrepare = Flow.fromGraph(new GCMDispatcherPrepare)
    val gcmRHandler = Flow.fromGraph(new GCMResponseHandler)
    val metrics = Flow.fromGraph(new FlowMetrics[fkint.mp.connekt.PNCallbackEvent](Channel.PUSH))
    val eventCreator = Flow.fromGraph(new PNBigfootEventCreator)
    val qps = meter("android.send")

    val complexSource = Source.fromGraph(GraphDSL.create() { implicit b =>
      val merge = b.add(Merge[ConnektRequest](3))
      kSource ~> merge.in(0)
      Source.empty[ConnektRequest] ~> merge.in(1)
      Source.empty[ConnektRequest] ~> merge.in(2)

      SourceShape(merge.out)
    })

    val gWithRHandler = complexSource.via(render).via(gcmFmt).via(gcmPrepare).via(HttpDispatcher.gcmPoolClientFlow).via(gcmRHandler).via(eventCreator).via(metrics).to(Sink.foreach(e => {
      qps.mark()
      if (0 == (counter.incrementAndGet() % 1000)) {
        val currentTime = System.currentTimeMillis()
        val rate = 1000000 / (currentTime - prevTime.getAndSet(currentTime))
        println(s"FlatAndroidBenchmarkTopology #Rate: [$rate], MR[${qps.getMeanRate}}], 1MR[${qps.getOneMinuteRate}}] upto ${counter.get()} messages by $currentTime")
      }
    }))

    gWithRHandler.run()

    Await.result(Promise[String]().future, 400.seconds)
  }

}
