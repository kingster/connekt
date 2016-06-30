package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.node.ObjectNode

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "message_type"
)
@JsonSubTypes(Array(
  new Type(value = classOf[XmppReceipt], name = "receipt")
))
abstract case class XmppUpstreamResponse (
                              @JsonProperty("message_id")@JsonProperty(required = true) messageId: String,
                              @JsonProperty(required = true) from: String,
                              @JsonProperty(required = true) category: String)
