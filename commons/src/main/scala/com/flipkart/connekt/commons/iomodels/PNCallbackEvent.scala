package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
case class PNCallbackEvent(@JsonProperty(required = false) messageId: String,
                           @JsonProperty(required = false) deviceId: String,
                           eventType: String,
                           @JsonProperty(required = false) platform: String,
                           @JsonProperty(required = false) appName: String,
                           contextId: String,
                           @JsonProperty(required = false) cargo: String,
                           timestamp: Long) extends CallbackEvent
