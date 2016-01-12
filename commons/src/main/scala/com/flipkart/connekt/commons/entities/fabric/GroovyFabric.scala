package com.flipkart.connekt.commons.entities.fabric

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
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
