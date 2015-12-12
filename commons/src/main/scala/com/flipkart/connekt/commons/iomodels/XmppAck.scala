package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
class XmppAck(@JsonProperty("message_type") messageType: String,
              @JsonProperty("message_id") messageId: String,
              @JsonProperty(required = false) from: String) extends XmppResponse {
  override def responseType(): String = messageType
}

