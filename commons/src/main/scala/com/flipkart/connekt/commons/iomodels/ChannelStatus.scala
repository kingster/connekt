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
new Type(value = classOf[PNStatus], name = "PN"),
new Type(value = classOf[GCardStatus], name = "GCard")
))
abstract class ChannelStatus