package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
@JsonTypeInfo(
use = JsonTypeInfo.Id.NAME,
include = JsonTypeInfo.As.PROPERTY,
property = "type"
)
@JsonSubTypes(Array(
new Type(value = classOf[PNRequestData], name = "PN"),
new Type(value = classOf[GCardRequestData], name = "GCard"),
new Type(value = classOf[EmailRequestData], name="Email")
))
abstract class ChannelRequestData
