package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.Stencil

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
trait TStencilDao extends Dao {

  def getStencil(id: String): Option[Stencil]

  def upsertStencil(stencil: Stencil): Try[Unit]

}
