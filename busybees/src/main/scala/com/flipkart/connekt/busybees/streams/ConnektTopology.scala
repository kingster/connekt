package com.flipkart.connekt.busybees.streams

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, Attributes, Materializer}
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

  def graph() = source.withAttributes(ActorAttributes.dispatcher("akka.actor.default-pinned-dispatcher")).via(transform).to(sink)
//    .withAttributes(Attributes.inputBuffer(initial = 16, max = 64))
    .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel, onFinish = Logging.InfoLevel, onFailure = Logging.ErrorLevel))

  def run(implicit mat: Materializer) = graph().run()
  def shutdown()
}
