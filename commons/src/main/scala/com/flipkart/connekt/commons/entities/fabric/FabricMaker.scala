/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.entities.fabric

import com.roundeights.hasher.Implicits._
import groovy.lang.GroovyClassLoader

object FabricMaker {

  /**
   *
   * @param groovyFabric groovy class content
   * @param cTag implicit erased class of type T
   * @tparam T classType of groovy class
   * @return groovy class instance created
   */
  def create[T](stencilId: String, groovyFabric: String)(implicit cTag: reflect.ClassTag[T]): T = {
    val groovyFabricKey = s"""G${groovyFabric.md5.hash.hex}"""
    val gcl: GroovyClassLoader = new GroovyClassLoader()
    gcl.parseClass(groovyFabric, groovyFabricKey).newInstance().asInstanceOf[T]
  }

  def createVtlFabric(objRep: String): VelocityFabric = {
    new VelocityFabric(objRep)
  }
}
