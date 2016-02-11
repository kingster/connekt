package com.flipkart.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics._
import com.flipkart.utils.NetworkUtils

/**
 * Created by kinshuk.bairagi on 11/02/16.
 */
object MetricRegistry {

  val REGISTRY: MetricRegistry = new com.codahale.metrics.MetricRegistry()

  val jmxReporter: JmxReporter = JmxReporter
    .forRegistry(REGISTRY)
    .inDomain("fk.metrics")
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .convertRatesTo(TimeUnit.SECONDS)
    .build()

  jmxReporter.start()

  // Console debug of metrics.
  if (NetworkUtils.getHostname.contains("local") && Option(System.getProperty("metric.debug")).getOrElse("false").toBoolean) {
    val consoleReporter = ConsoleReporter.forRegistry(REGISTRY)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build()
    consoleReporter.start(60, TimeUnit.SECONDS)
  }


}


