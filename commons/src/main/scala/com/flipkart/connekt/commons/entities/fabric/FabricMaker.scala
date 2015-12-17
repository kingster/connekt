package com.flipkart.connekt.commons.entities.fabric

import java.util.concurrent.{Callable, TimeUnit}

import com.flipkart.connekt.commons.utils.StringUtils._
import com.google.common.cache.{Cache, CacheBuilder}
import groovy.lang.GroovyClassLoader

/**
 *
 *
 * @author durga.s
 * @version 12/15/15
 */
object FabricMaker {

  val fabricCache: Cache[String, EngineFabric] =
    CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterAccess(6, TimeUnit.HOURS)
      .asInstanceOf[CacheBuilder[String, EngineFabric]]
      .recordStats()
      .build[String, EngineFabric]()

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

    fabricCache.get(groovyFabricKey, new Callable[GroovyFabric](){
      override def call(): GroovyFabric = {
        val gcl: GroovyClassLoader = new GroovyClassLoader()
        gcl.parseClass(groovyFabric, groovyFabricKey).newInstance().asInstanceOf[T]
      }
    }).asInstanceOf[T]
  }

  def createEmailVtlFabric(stencilId: String, objRep: String): EmailVelocityFabric = {
    val vtlKey = s"""V${md5(objRep)}"""
    fabricCache.get(vtlKey, new Callable[EmailVelocityFabric](){
      override def call(): EmailVelocityFabric = {
        objRep.getObj[EmailVelocityFabric]
      }
    }).asInstanceOf[EmailVelocityFabric]
  }


  def createPNVtlFabric(stencilId: String, objRep: String): PNVelocityFabric = {
    val vtlKey = s"""V${md5(objRep)}"""
    fabricCache.get(vtlKey, new Callable[PNVelocityFabric](){
      override def call(): PNVelocityFabric = {
        objRep.getObj[PNVelocityFabric]
      }
    }).asInstanceOf[PNVelocityFabric]
  }
}
