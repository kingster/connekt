/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.busybees.models

import com.flipkart.connekt.commons.iomodels.WNSPayloadEnvelope

case class WNSRequestTracker(appName: String, requestId: String, request: WNSPayloadEnvelope) extends HTTPRequestTracker
