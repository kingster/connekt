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
property = "eType"
)
@JsonSubTypes(Array(
new Type(value = classOf[VelocityFabric], name = "VELOCITY"),
new Type(value = classOf[GroovyFabric], name = "GROOVY")
))
trait EngineFabric
