package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
case class XmppControl(@JsonProperty("message_type") messageType: String,
                       @JsonProperty("control_type") controlType: String) extends XmppResponse {
  override def responseType(): String = messageType
}