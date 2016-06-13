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
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.{Bucket, Stencil}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, TMySQLFactory}

class StencilDao(registryTable: String, tableName: String, historyTableName: String, bucketRegistryTable: String, jdbcHelper: TMySQLFactory) extends TStencilDao with MySQLDao {

  val mysqlHelper = jdbcHelper

  override def getStencil(id: String, version: Option[String] = None): List[Stencil] = {
    implicit val j = mysqlHelper.getJDBCInterface
    try {
      val q1 =
        s"""
           |SELECT * FROM $historyTableName as h, $registryTable as r WHERE  r.id = h.id AND id = ? and version = ?
            """.stripMargin

      val q2 =
        s"""
           |SELECT * FROM $tableName as t, $registryTable as r WHERE r.id = t.id AND id = ?
            """.stripMargin

      version.map(queryForList[Stencil](q1, id, _)).getOrElse(queryForList[Stencil](q2, id))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching stencil [$id] ${e.getMessage}", e)
        throw e
    }
  }

  override def getStencilByName(name: String, version: Option[String] = None): List[Stencil] = {
    implicit val j = mysqlHelper.getJDBCInterface
    try {
      val q1 =
        s"""
           |SELECT * FROM $historyTableName as h, $registryTable as r WHERE name = ? and version = ? and r.id = h.id
            """.stripMargin

      val q2 =
        s"""
           |SELECT * FROM $tableName as t, $registryTable as r WHERE name = ? and r.id = t.id
            """.stripMargin

      version.map(queryForList[Stencil](q1, name, _)).getOrElse(queryForList[Stencil](q2, name))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching stencil [$name] ${e.getMessage}", e)
        throw e
    }
  }


  override def writeStencil(stencil: Stencil): Unit = {
    implicit val j = mysqlHelper.getJDBCInterface

    val q0 =
      s"""
         |INSERT INTO $tableName (id, name
                                  |ON DUPLICATE KEY UPDATE name = ?
      """.stripMargin

    val q1 =
      s"""
         |INSERT INTO $tableName (id, tag, engine, engineFabric, createdBy, updatedBy, version, bucket) VALUES(?, ?, ?, ?, ?, ?, ?)
                                  |ON DUPLICATE KEY UPDATE engine = ?, engineFabric = ?, updatedBy = ?, version = version + 1, bucket = ?
      """.stripMargin

    val q2 =
      s"""
         |INSERT INTO $historyTableName (id, tag, name, engine, engineFabric, createdBy, updatedBy, version, bucket) VALUES(?, ?, ?, ?, ?, (SELECT VERSION from $tableName where id = ?), ?)
      """.stripMargin

    try {
      update(q1, stencil.id, stencil.name)
      update(q1, stencil.id, stencil.tag, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.updatedBy, stencil.version.toString, stencil.bucket, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.bucket)
      update(q2, stencil.id, stencil.tag, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.updatedBy, stencil.id, stencil.bucket)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error updating stencil [${stencil.id}] ${e.getMessage}", e)
        throw e
    }
  }

  override def getBucket(name: String): Option[Bucket] = {
    implicit val j = mysqlHelper.getJDBCInterface

    try {
      val q =
        s"""
           |SELECT * FROM $bucketRegistryTable WHERE name = ?
            """.stripMargin
      query[Bucket](q, name)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching bucket [$name] ${e.getMessage}", e)
        throw e
    }
  }

  override def writeBucket(bucket: Bucket): Unit = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |INSERT INTO $bucketRegistryTable (id, name) VALUES(?, ?)
      """.stripMargin

    try {
      update(q, bucket.id, bucket.name)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error updating stencil [${bucket.name}}] ${e.getMessage}", e)
        throw e
    }
  }
}

object StencilDao {
  def apply(registryTable: String, tableName: String, historyTableName: String, bucketRegistryTable: String, jdbcHelper: TMySQLFactory) =
    new StencilDao(registryTable: String, tableName: String, historyTableName: String, bucketRegistryTable: String, jdbcHelper: TMySQLFactory)
}
