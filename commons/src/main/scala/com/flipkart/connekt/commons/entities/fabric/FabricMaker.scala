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
   * @param groovyFabric groovy class content
   * @param groovyClassName className of groovy class to initialise
   * @param cTag implicit erased class of type T
   * @tparam T classType of groovy class
   * @return groovy class instance created
   */
  def create[T <: GroovyFabric](groovyFabric: String, groovyClassName: String)(implicit cTag: reflect.ClassTag[T]): T = {
    val gcl: GroovyClassLoader = new GroovyClassLoader()
    gcl.parseClass(groovyFabric, groovyClassName).newInstance().asInstanceOf[T]
  }

  def createEmailVtlFabric(stencilId: String /* _cache by id, for reuse ?_ */, objRep: String): EmailVelocityFabric =
    objRep.getObj[EmailVelocityFabric]

  def createPNVtlFabric(stencilId: String /* _cache by id, for reuse ?_ */, objRep: String): PNVelocityFabric =
    objRep.getObj[PNVelocityFabric]
}
