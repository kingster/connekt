package com.flipkart.connekt.commons.metrics

import com.codahale.metrics.MetricRegistry
import com.flipkart.metrics.InstrumentedBase

/**
 * Created by kinshuk.bairagi on 11/02/16.
 */
trait Instrumented extends InstrumentedBase {

  val registry:MetricRegistry = MetricRegistry.REGISTRY

}

