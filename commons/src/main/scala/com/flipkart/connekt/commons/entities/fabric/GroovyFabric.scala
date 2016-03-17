/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities.fabric

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(
use = JsonTypeInfo.Id.NAME,
include = JsonTypeInfo.As.PROPERTY,
property = "cType"
)
@JsonSubTypes(Array(
new Type(value = classOf[PNGroovyFabric], name = "PN"),
new Type(value = classOf[EmailGroovyFabric], name = "EMAIL")
))
trait GroovyFabric extends EngineFabric {
}

abstract class PNGroovyFabric extends GroovyFabric with PNFabric {}

abstract class EmailGroovyFabric extends GroovyFabric with EmailFabric {}
