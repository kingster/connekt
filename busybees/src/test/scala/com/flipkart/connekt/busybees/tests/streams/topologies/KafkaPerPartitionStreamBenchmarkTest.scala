/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.busybees.tests.streams.topologies

import java.util.concurrent.atomic.AtomicLong

import akka.NotUsed
import akka.stream.scaladsl.{GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorAttributes, SourceShape}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.metrics.Instrumented
import org.scalatest.Ignore

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import akka.stream.scaladsl.GraphDSL.Implicits._

@Ignore
class KafkaPerPartitionStreamBenchmarkTest extends TopologyUTSpec with Instrumented {

  def source: Source[ConnektRequest, NotUsed] = Source.fromGraph(GraphDSL.create(){ implicit b =>

    val topic = "push_connekt_insomnia_d346b56a260f1a"
    ConnektLogger(LogFile.PROCESSORS).info(s"Creating composite source for topics: $topic")

    val merge = b.add(Merge[ConnektRequest](10))
    for(portNum <- 0 to 9)
      new KafkaSource[ConnektRequest](getKafkaConsumerConf, topic, getKafkaConsumerConf.getString("group.id"))(Promise[String]().future) ~> merge.in(portNum)

    SourceShape(merge.out)
  })


  "KafkaPerPartitionStreamBenchmarkTest" should "bench" in {

    ConnektLogger(LogFile.PROCESSORS).info("Starting KafkaPerPartitionStreamBenchmarkTest.")

    val qps = meter("kafa.read")
    val counter: AtomicLong = new AtomicLong(0)

    //Run the benchmark topology
    val kafkaCompositeSource = source.withAttributes(ActorAttributes.dispatcher("akka.actor.default-pinned-dispatcher"))
    val rF = kafkaCompositeSource.runWith(Sink.foreach( r => {
      qps.mark()
      if(0 == (counter.incrementAndGet() % 1000)) {
        ConnektLogger(LogFile.PROCESSORS).info(s">>> MR[${qps.getMeanRate}], 1MR[${qps.getOneMinuteRate}], 5MR[${qps.getFiveMinuteRate}]")
      }
    }))

    Await.result(rF, 500.seconds)
  }
}
