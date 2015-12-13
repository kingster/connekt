package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.Stencil

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
trait TStencilDao {
  
  def getStencil(id: String): Stencil
  def updateStencil(stencil: Stencil): Unit
}
