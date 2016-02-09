package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.flipkart.connekt.commons.utils.DateTimeUtils

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
                           timestamp: Long) extends CallbackEvent {

  def toBigfootEntity : fkint.mp.connekt.PNCallbackEvent = {
    fkint.mp.connekt.PNCallbackEvent(messageId = messageId, deviceId = deviceId, platform = platform, eventType = eventType, appName = appName, contextId = contextId, cargo = cargo, timestamp = DateTimeUtils.getStandardFormatted(timestamp))
  }
  
}
