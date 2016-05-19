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
package com.flipkart.connekt.busybees.streams.flows.profilers

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{BidiFlow, Flow, GraphDSL}
import akka.stream.{BidiShape, FlowShape}
import com.flipkart.connekt.busybees.models.RequestTracker
import com.flipkart.connekt.commons.metrics.Instrumented
import scala.collection.JavaConverters._
import scala.util.Try

object TimedFlowOps {

  implicit class TimedFlow[I, O,  T <: RequestTracker, M](dispatchFlow: Flow[(I, T), (Try[O], T), M]) extends Instrumented {

    val startTimes = new ConcurrentHashMap[T, Long]().asScala

    private def profilingShape(apiName: String) = BidiFlow.fromGraph(GraphDSL.create() { implicit b =>

      val out = b.add(Flow[(I, T)].map {
        case (request, requestTracker) =>
          startTimes.put(requestTracker, System.currentTimeMillis())
          (request, requestTracker)
      })

      val in = b.add(Flow[(Try[O], T)].map {
        case (response, httpRequestTracker) =>
          startTimes.get(httpRequestTracker).map(start => {
            startTimes.remove(httpRequestTracker)
            System.currentTimeMillis() - start
          }).foreach(registry.timer(getMetricName(apiName)).update(_, TimeUnit.MILLISECONDS))

          (response, httpRequestTracker)
      })

      BidiShape.fromFlows(out, in)
    })

    def timedAs(apiName: String) = Flow.fromGraph(GraphDSL.create() { implicit b =>
      val s = b.add(profilingShape(apiName))
      val p = b.add(dispatchFlow)

      s.out1 ~> p ~> s.in2

      FlowShape(s.in1, s.out2)
    })
  }
}
