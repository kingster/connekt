package com.flipkart.connekt.commons.entities

import javax.persistence.Column

import com.flipkart.connekt.commons.entities.StencilEngine.StencilEngine

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
class Stencil() {
  @Column(name = "id")
  var id: String = _

  @EnumTypeHint(value = "com.flipkart.connekt.commons.entities.StencilEngine")
  @Column(name = "engine")
  var engine: StencilEngine.StencilEngine = StencilEngine.GROOVY

  @Column(name = "engineFabric")
  var engineFabric: String = _

  def this(id: String, engine: StencilEngine, engineFabric: String) = {
    this
    this.id = id
    this.engine = engine
    this.engineFabric = engineFabric
  }
}

object StencilEngine extends Enumeration {
  type StencilEngine = Value
  val VELOCITY, GROOVY = Value
}
