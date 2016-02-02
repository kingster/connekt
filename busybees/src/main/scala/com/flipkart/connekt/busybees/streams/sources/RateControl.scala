package com.flipkart.connekt.busybees.streams.sources

import java.util.concurrent.TimeUnit

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import org.isomorphism.util.TokenBuckets

import scala.reflect.ClassTag

/**
 * Created by kinshuk.bairagi on 02/02/16.
 *
 * @param capacity
 * @param tokenRefreshPeriod
 * @param tokenRefreshAmount
 * @tparam V
 */
class RateControl[V: ClassTag](capacity: Long, tokenRefreshPeriod: Long, tokenRefreshAmount: Long) extends GraphStage[FlowShape[V, V]] {

  val in = Inlet[V]("RateControl.In")
  val out = Outlet[V]("RateControl.Out")

  override def shape: FlowShape[V, V] = FlowShape.of(in, out)

  val tokenBucket = TokenBuckets.builder().withCapacity(capacity)
    .withFixedIntervalRefillStrategy(tokenRefreshAmount, tokenRefreshPeriod, TimeUnit.SECONDS).build()

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"RateControl:: onPush")
        if (tokenBucket.tryConsume(1)) {
          val message = grab(in)
          ConnektLogger(LogFile.PROCESSORS).info(s"RateControl:: onPush:: Message ${message.toString}")
          push(out, message)
        } else {
          ConnektLogger(LogFile.PROCESSORS).warn("RateControl :: Rate Limited..")
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"RateControl:: onPull.")
        pull(in)
      }
    })

  }

}
