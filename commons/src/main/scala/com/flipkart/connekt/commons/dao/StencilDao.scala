package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.entities.Stencil
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
class StencilDao(tableName: String, jdbcHelper: MySQLFactory) extends TStencilDao with MySQLDao {
  val mysqlHelper = jdbcHelper

  override def getStencil(id: String): Option[Stencil] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
        |SELECT * FROM $tableName WHERE id = ?
      """.stripMargin

    try {
      query[Stencil](q, id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching stencil [$id] ${e.getMessage}", e)
        throw e
    }
  }

  override def updateStencil(stencil: Stencil): Unit = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
        |INSERT INTO $tableName (id, engine, engineFabric) VALUES(?, ?, ?)
        |ON DUPLICATE KEY UPDATE engine = ?, engineFabric = ?
      """.stripMargin

    try {
      update(q, stencil.id, stencil.engine, stencil.engineFabric, stencil.engine, stencil.engineFabric)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error updating stencil [${stencil.id}}] ${e.getMessage}", e)
        throw e
    }
  }
}

object StencilDao {
  def apply(tableName: String, jdbcHelper: MySQLFactory) =
    new StencilDao(tableName: String, jdbcHelper: MySQLFactory)
}
