/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "message_type"
)
@JsonSubTypes(Array(
  new Type(value = classOf[XmppAck], name = "ack"),
  new Type(value = classOf[XmppNack], name = "nack"),
  new Type(value = classOf[XmppReceipt], name = "receipt"),
  new Type(value = classOf[XmppControl], name = "control")
))
abstract class XmppResponse {
  def responseType(): String
}
