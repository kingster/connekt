package com.flipkart.connekt.busybees.models

import com.flipkart.connekt.commons.iomodels.WNSPayloadEnvelope

/**
 *
 *
 * @author durga.s
 * @version 2/2/16
 */
case class WNSRequestTracker(appName: String, requestId: String, request: WNSPayloadEnvelope) extends HTTPRequestTracker
