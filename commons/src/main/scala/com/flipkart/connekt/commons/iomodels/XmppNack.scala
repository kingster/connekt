package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
case class XmppNack(@JsonProperty("message_type") messageType: String,
                        @JsonProperty("message_id")@JsonProperty(required = false) messageId: String,
                        @JsonProperty(required = false) from: String,
                        @JsonProperty(required = false) error: String,
                        @JsonProperty("error_description")@JsonProperty(required = false) errorDescription: String) extends XmppResponse {
  override def responseType(): String = messageType
}
