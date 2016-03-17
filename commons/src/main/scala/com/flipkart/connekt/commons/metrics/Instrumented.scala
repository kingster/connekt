/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.metrics

import com.flipkart.metrics.InstrumentedBase

trait Instrumented extends InstrumentedBase {

  val registry = MetricRegistry.REGISTRY

}
