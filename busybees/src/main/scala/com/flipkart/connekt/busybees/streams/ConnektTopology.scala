package com.flipkart.connekt.busybees.streams

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Flow, Source}
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
  def run(implicit mat: Materializer) = graph().run()
  def shutdown()
}
