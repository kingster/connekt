package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.flipkart.connekt.commons.iomodels.XmppDownstreamResponse

/**
 * Created by subir.dey on 24/06/16.
 */
case class XmppUpstreamAck (@JsonProperty("message_id") messageId: String,
  @JsonProperty("message_type") messageType: String,
  @JsonProperty(required = true) to: String)
