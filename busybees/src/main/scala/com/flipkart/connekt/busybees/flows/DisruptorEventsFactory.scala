package com.flipkart.connekt.busybees.flows

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.lmax.disruptor.EventFactory
import kafka.consumer.ConsumerConnector

/**
 *
 *
 * @author durga.s
 * @version 12/3/15
 */
class DisruptorEventsFactory[T](kafkaConnector: ConsumerConnector, topic: String, numStreams: Int) extends EventFactory[T] {

  lazy val topicStream = Stream.empty[T]

  lazy val iterator: Iterator[T] = topicStream.iterator

  lazy val streams = kafkaConnector.createMessageStreams(Map[String, Int](topic -> numStreams))
  streams.keys.foreach(topic => {
    ConnektLogger(LogFile.FACTORY).info(s"Initializing disruptor events' factory for topic: ${topic}")
    streams.get(topic).map(_.zipWithIndex).foreach(l => {
      l.foreach(x => {
        topicStream.append(x._1)
      })
    })
  })

  override def newInstance(): T = iterator.next()
}
