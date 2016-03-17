/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

case class XmppControl(@JsonProperty("message_type") messageType: String,
                       @JsonProperty("control_type") controlType: String) extends XmppResponse {
  override def responseType(): String = messageType
}
