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
package com.flipkart.connekt.commons.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics._

object MetricRegistry {

  val REGISTRY = new com.codahale.metrics.MetricRegistry()

  val jmxReporter: JmxReporter = JmxReporter
    .forRegistry(REGISTRY)
    .inDomain("fk.metrics")
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .convertRatesTo(TimeUnit.SECONDS)
    .build()

  jmxReporter.start()

  // Console debug of metrics.
  if (Option(System.getProperty("metric.debug")).getOrElse("false").toBoolean) {
    val consoleReporter = ConsoleReporter.forRegistry(REGISTRY)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build()
    consoleReporter.start(60, TimeUnit.SECONDS)
  }


}
