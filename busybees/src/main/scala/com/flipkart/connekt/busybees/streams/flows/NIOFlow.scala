/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.busybees.streams.flows

import akka.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContextExecutor, Future}

abstract class NIOFlow[In, Out](parallelism: Int)(ec: ExecutionContextExecutor) {

  def map: In => List[Out]

  def flow = Flow[In].map(identity).mapAsync(parallelism){
    i => Future(map(i))(ec)
  }.mapConcat(identity)
}
