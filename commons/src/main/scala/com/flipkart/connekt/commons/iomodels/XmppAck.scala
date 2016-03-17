/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

class XmppAck(@JsonProperty("message_type") messageType: String,
              @JsonProperty("message_id") messageId: String,
              @JsonProperty(required = false) from: String) extends XmppResponse {
  override def responseType(): String = messageType
}
