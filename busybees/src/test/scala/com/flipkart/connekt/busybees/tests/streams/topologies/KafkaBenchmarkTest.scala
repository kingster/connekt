package com.flipkart.connekt.busybees.tests.streams.topologies

import java.util.concurrent.atomic.AtomicLong

import akka.stream.scaladsl.{Sink, Source}
import com.flipkart.connekt.busybees.streams.sources.{KafkaSource, MessageDecoder}
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.StringUtils._
import com.softwaremill.react.kafka.{ConsumerProperties, ReactiveKafka}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

/**
 *
 *
 * @author durga.s
 * @version 3/10/16
 */
//@Ignore
class KafkaBenchmarkTest extends TopologyUTSpec with Instrumented {

  val counter: AtomicLong = new AtomicLong(0)
  val counterTS: AtomicLong = new AtomicLong(System.currentTimeMillis)

  "KafkaBenchmarkTest" should "bench" in {

    val kSource = Source.fromGraph(new KafkaSource[ConnektRequest](getKafkaConsumerHelper, "push_connekt_insomnia_d346b56a260f1a")(Promise[String]().future))
    val qps = meter("kafa.read")

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


    /*val rKafka = new ReactiveKafka()
    val publisher = rKafka.consume(ConsumerProperties(
      brokerList = "10.84.174.216:9092",
      zooKeeperHost = "10.84.174.225:2181,10.84.174.222:2181/bro/kafka-nm-qa",
      topic = "push_connekt_insomnia_d346b56a260f1a",
      groupId = "ckt",
      decoder = new MessageDecoder[ConnektRequest]()
    ))
    val reactiveSource = Source.fromPublisher(publisher).map(_.message())
    */

    //Run the benchmark topology
    val rF = kSource.runWith(Sink.foreach( r => {
      qps.mark()
      if(0 == (counter.incrementAndGet() % 1000)) {
        ConnektLogger(LogFile.SERVICE).info(s">>> MR[${qps.getMeanRate}], 1MR[${qps.getOneMinuteRate}], 5MR[${qps.getFiveMinuteRate}]")
      }
    }))

    Await.result(rF, 500.seconds)
  }


}
