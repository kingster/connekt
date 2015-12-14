package com.flipkart.connekt.commons.entities.fabric

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import groovy.lang.{GroovyCodeSource, GroovyClassLoader}

import scala.util.Try

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
  def validateGroovy(): Try[Boolean]
}

trait PNGroovyFabric extends GroovyFabric with PNFabric {
  override def validateGroovy(): Try[Boolean] = Try.apply(true)
}

trait EmailGroovyFabric extends GroovyFabric with EmailFabric {
  override def validateGroovy(): Try[Boolean] = Try.apply(true)
}
