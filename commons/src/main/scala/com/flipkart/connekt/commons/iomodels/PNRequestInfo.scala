/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

case class PNRequestInfo(@JsonProperty(required = false) platform: String,
                         @JsonProperty(required = false) appName: String,
                         @JsonProperty(required = false) deviceId: List[String] = List.empty[String],
                         ackRequired: Boolean,
                         delayWhileIdle: Boolean) extends ChannelRequestInfo {
  def this() {
    this(null, null, List.empty[String], false, false)
  }
}
