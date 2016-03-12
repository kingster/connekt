package com.flipkart.connekt.busybees.streams.flows

import akka.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 *
 *
 * @author durga.s
 * @version 3/12/16
 */
abstract class NIOFlow[I, O](parallelism: Int)(ec: ExecutionContextExecutor) {

  def map: I => List[O]

  val flow = Flow[I].map(identity).mapAsync(parallelism){
    i => Future(map(i))(ec)
  }.mapConcat(identity)
}
