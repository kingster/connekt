package com.flipkart.connekt.commons.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics._
import com.flipkart.utils.NetworkUtils

/**
 * Created by kinshuk.bairagi on 11/02/16.
 */
object ConnektMetricRegistry {

  val REGISTRY: MetricRegistry = new com.codahale.metrics.MetricRegistry()

  val jmxReporter: JmxReporter = JmxReporter
    .forRegistry(REGISTRY)
    .inDomain("connekt.metrics")
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .convertRatesTo(TimeUnit.SECONDS)
    .build()

  jmxReporter.start()

  //  Comment out when the metrics is stable.
  if (NetworkUtils.getHostname.contains("local")) {
    val consoleReporter = ConsoleReporter.forRegistry(REGISTRY)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build()
    consoleReporter.start(30, TimeUnit.SECONDS)
  }

}


trait Instrumented {

  protected val registry = ConnektMetricRegistry.REGISTRY

  protected def getMetricName(name: String): String = MetricRegistry.name(getClass, name)

  def profile[T](metricName: String)(fn: ⇒ T): T = {
    val context = registry.timer(getMetricName(metricName)).time()
    try {
      fn
    } finally {
      context.stop()
    }
  }

  def counter(metricName: String): Counter = registry.counter(getMetricName(metricName))

  def meter(metricName: String): Meter = registry.meter(getMetricName(metricName))

}

