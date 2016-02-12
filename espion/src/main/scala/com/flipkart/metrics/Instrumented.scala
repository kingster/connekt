package com.flipkart.metrics

import com.codahale.metrics.{Meter, Counter}

/**
 * Created by kinshuk.bairagi on 11/02/16.
 */
trait Instrumented {

  protected val registry = MetricRegistry.REGISTRY

  protected def getMetricName(name: String): String = com.codahale.metrics.MetricRegistry.name(getClass, name).replaceAll("$","")

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

