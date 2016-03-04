package com.flipkart.connekt.commons.entities.fabric

import com.flipkart.connekt.commons.utils.StringUtils._
import groovy.lang.GroovyClassLoader

/**
 *
 *
 * @author durga.s
 * @version 12/15/15
 */
object FabricMaker {

  /**
   *
   * @param stencilId stencil identifier
   * @param groovyFabric groovy class content
   * @param cTag implicit erased class of type T
   * @tparam T classType of groovy class
   * @return groovy class instance created
   */
  def create[T <: GroovyFabric](stencilId: String, groovyFabric: String)(implicit cTag: reflect.ClassTag[T]): T = {
    val groovyFabricKey = s"""G${md5(groovyFabric)}"""
    val gcl: GroovyClassLoader = new GroovyClassLoader()
    gcl.parseClass(groovyFabric, groovyFabricKey).newInstance().asInstanceOf[T]


  }

  def createVtlFabric(stencilId: String, objRep: String): VelocityFabric = {
    objRep.getObj[VelocityFabric]
  }
}
