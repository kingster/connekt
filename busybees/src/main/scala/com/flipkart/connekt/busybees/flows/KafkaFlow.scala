package com.flipkart.connekt.busybees.flows

import akka.stream.SourceShape
import akka.stream.scaladsl.{FlowGraph, Merge, Source}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import kafka.consumer.ConsumerConnector
import kafka.message.MessageAndMetadata
import org.apache.commons.codec.CharEncoding

/**
 *
 *
 * @author durga.s
 * @version 12/2/15
 */
@deprecated("akka-streams is an elegant solution, however, understanding dsl, stream-components might be blocker for team.")
trait KafkaFlow {

  def getSource(kafkaConnector: ConsumerConnector, topic: String, numStreams: Int): Source[String, Unit] = {

    val g = FlowGraph.create() { implicit builder: FlowGraph.Builder[Unit] =>
      import akka.stream.scaladsl.FlowGraph.Implicits._

      val merge = builder.add(Merge[MessageAndMetadata[Array[Byte], Array[Byte]]](numStreams))

      val streams = kafkaConnector.createMessageStreams(Map[String, Int](topic -> numStreams))
      streams.keys.foreach(topic => {
        ConnektLogger(LogFile.SERVICE).debug("KafkaFlow: For topic: %s".format(topic))
        streams.get(topic).map(_.zipWithIndex).foreach(l => {
          l.foreach(x => {
            val messageAndMetaIterator: Iterator[MessageAndMetadata[Array[Byte], Array[Byte]]] = x._1.iterator()
            Source(() => messageAndMetaIterator) ~> merge
            ConnektLogger(LogFile.SERVICE).debug("merging stream %d".format(x._2))
          })
        })
      })

      SourceShape(merge.out)
    }

    Source.fromGraph(g).map[String](b => new String(b.message, CharEncoding.UTF_8))
  }
}
