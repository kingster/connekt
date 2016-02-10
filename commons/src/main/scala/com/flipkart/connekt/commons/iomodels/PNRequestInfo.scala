package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
case class PNRequestInfo(@JsonProperty(required = false) platform: String,
                         @JsonProperty(required = false) appName: String,
                         @JsonProperty(required = false) deviceId: List[String],
                         ackRequired: Boolean,
                         delayWhileIdle: Boolean) extends ChannelRequestInfo
