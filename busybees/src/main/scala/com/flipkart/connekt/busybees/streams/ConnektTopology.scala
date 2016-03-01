package com.flipkart.connekt.busybees.streams

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Attributes, Materializer}
import com.flipkart.connekt.commons.iomodels.{CallbackEvent, ConnektRequest}

/**
 *
 *
 * @author durga.s
 * @version 2/25/16
 */
trait ConnektTopology[E <:CallbackEvent] {
  def source: Source[ConnektRequest, NotUsed]
  def transform: Flow[ConnektRequest, E, NotUsed]
  def sink: Sink[E, NotUsed]

  def graph() = source.via(transform).to(sink)
    /*.withAttributes(Attributes.inputBuffer(initial = 1, max = 1))*/
    .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel, onFinish = Logging.InfoLevel, onFailure = Logging.ErrorLevel))

  def run(implicit mat: Materializer) = graph().run()
  def shutdown()
}
