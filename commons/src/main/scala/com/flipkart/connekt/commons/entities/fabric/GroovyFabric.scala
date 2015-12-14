package com.flipkart.connekt.commons.entities.fabric

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import groovy.lang.GroovyClassLoader

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
sealed abstract class GroovyFabric extends EngineFabric {
  /**
   *
   * @param groovyFabric groovy class content
   * @param groovyClassName className of groovy class to initialise
   * @param cTag implicit erased class of type T
   * @tparam T classType of groovy class
   * @return groovy class instance created
   */
  def fabricate[T <: GroovyFabric](groovyFabric: String, groovyClassName: String)(implicit cTag: reflect.ClassTag[T]): T = {
    val gcl: GroovyClassLoader = new GroovyClassLoader()
    gcl.parseClass(groovyFabric, groovyClassName).newInstance().asInstanceOf[T]
  }

  def validateGroovy(): Try[Boolean]
}

abstract class PNGroovyFabric(pnGroovy: String) extends GroovyFabric with PNFabric {}

abstract class EmailGroovyFabric(emailGroovy: String) extends GroovyFabric with EmailFabric {}