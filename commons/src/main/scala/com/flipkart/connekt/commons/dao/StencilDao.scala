package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.entities.Stencil

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
class StencilDao(tableName: String, jdbcHelper: MySQLFactory) extends TStencilDao with MySQLDao {
  val mysqlHelper = jdbcHelper

  override def getStencil(id: String): Stencil = ???
  override def updateStencil(stencil: Stencil): Unit = ???
}
