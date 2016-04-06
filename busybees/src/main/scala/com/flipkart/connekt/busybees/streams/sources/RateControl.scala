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
package com.flipkart.connekt.busybees.streams.sources

import java.util.concurrent.TimeUnit

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import org.isomorphism.util.TokenBuckets

import scala.reflect.ClassTag

/**
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
    .withFixedIntervalRefillStrategy( tokenRefreshAmount, tokenRefreshPeriod, TimeUnit.SECONDS).build()

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {
        val message = grab(in)
        tokenBucket.consume(1)
        ConnektLogger(LogFile.PROCESSORS).trace(s"RateControl on push message ${message.toString}")
        if(isAvailable(out))
          push(out, message)
      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"RateControl error", e)
          if(!hasBeenPulled(in))
            pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if(!hasBeenPulled(in))
          pull(in)
      }
    })
  }
}
