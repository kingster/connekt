package com.flipkart.connekt.commons.entities

import org.apache.velocity.context.Context

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
sealed trait TStencilFabric {}

trait VelocityFabric extends TStencilFabric {
  /**
   *
   * @param context velocity engine operation context
   * @param vtlFabric input string containing the VTL to be rendered
   * @param errorTag identifier stencil name for log messages in case of error
   * @return output string of velocity rendering
   */
  def fabricate(context: Context, vtlFabric: String, errorTag: String): String
}

trait GroovyFabric extends TStencilFabric {
  /**
   *
   * @param groovyFabric groovy class content
   * @param groovyClassName className of groovy class to initialise
   * @param cTag implicit erased class of type T
   * @tparam T classType of groovy class
   * @return groovy class instance created
   */
  def fabricate[T](groovyFabric: String, groovyClassName: String)(implicit cTag: reflect.ClassTag[T]): T
}