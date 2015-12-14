package com.flipkart.connekt.commons.entities

import com.flipkart.connekt.commons.entities.StencilEngine.StencilEngine

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
case class Stencil(id: String, engine: StencilEngine, engineFabric: String) {}

object StencilEngine extends Enumeration {
  type StencilEngine = Value
  val VELOCITY, GROOVY = Value
}
