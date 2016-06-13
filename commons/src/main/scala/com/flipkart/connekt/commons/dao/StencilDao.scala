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

class StencilDao(tableName: String, historyTableName: String, bucketRegistryTable: String, jdbcHelper: TMySQLFactory) extends TStencilDao with MySQLDao {

  val mysqlHelper = jdbcHelper

  override def getStencil(id: String, version: Option[String] = None): List[Stencil] = {
    implicit val j = mysqlHelper.getJDBCInterface
    try {
      val q1 =
        s"""
           |SELECT * FROM $historyTableName WHERE id = ? and version = ?
            """.stripMargin

      val q2 =
        s"""
           |SELECT * FROM $tableName WHERE id = ?
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
           |SELECT * FROM $historyTableName WHERE name = ? and version = ?
            """.stripMargin

      val q2 =
        s"""
           |SELECT * FROM $tableName WHERE name = ?
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
    val q1 =
      s"""
         |INSERT INTO $tableName (id, name, tag, sType, engine, engineFabric, createdBy, updatedBy, version, bucket) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                  |ON DUPLICATE KEY UPDATE name = ?,tag = ?, sType = ?, engine = ?, engineFabric = ?, updatedBy = ?, version = version + 1, bucket = ?
      """.stripMargin

    val q2 =
      s"""
         |INSERT INTO $historyTableName (id, name, tag, sType, engine, engineFabric, createdBy, updatedBy, version, bucket) VALUES(?, ?, ?, ?, ?, ?, ?, ?, (SELECT VERSION from $tableName where id = ? and tag = ? ), ?)
      """.stripMargin

    try {
      update(q1, stencil.id, stencil.name, stencil.tag, stencil.sType, stencil.engine.toString, stencil.engineFabric, stencil.createdBy, stencil.updatedBy, stencil.version.toString, stencil.bucket, stencil.name, stencil.tag, stencil.sType, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.bucket)
      update(q2, stencil.id, stencil.name, stencil.tag, stencil.sType, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.updatedBy, stencil.id, stencil.tag, stencil.bucket)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error updating stencil [${stencil.id}] ${e.getMessage}", e)
        throw e
    }
  }

  override def updateStencilWithIdentity(prevName: String, stencil: Stencil): Unit = {

    implicit val j = mysqlHelper.getJDBCInterface
    j.execute("START TRANSACTION")
    deleteStencil(prevName, stencil)

    val q1 =
      s"""
         |INSERT INTO $tableName (id, name, tag, sType, engine, engineFabric, createdBy, updatedBy, version, bucket) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                  |ON DUPLICATE KEY UPDATE name = ?,tag = ?, sType = ?, engine = ?, engineFabric = ?, updatedBy = ?, version = version + 1, bucket = ?
      """.stripMargin

    val q2 =
      s"""
         |INSERT INTO $historyTableName (id, name, tag, sType, engine, engineFabric, createdBy, updatedBy, version, bucket) VALUES(?, ?, ?, ?, ?, ?, ?, ?, (SELECT VERSION from $tableName where id = ? and tag = ? ), ?)
      """.stripMargin

    try {
      update(q1, stencil.id, stencil.name, stencil.tag, stencil.sType, stencil.engine.toString, stencil.engineFabric, stencil.createdBy, stencil.updatedBy, stencil.version.toString, stencil.bucket, stencil.name, stencil.tag, stencil.sType, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.bucket)
      update(q2, stencil.id, stencil.name, stencil.tag, stencil.sType, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.updatedBy, stencil.id, stencil.tag, stencil.bucket)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error updating stencil [${stencil.id}] ${e.getMessage}", e)
        j.execute("ROLLBACK")
        throw e
    }
    j.execute("COMMIT")
  }

  override def deleteStencil(prevName: String, stencil: Stencil): Unit = {
    implicit val j = mysqlHelper.getJDBCInterface

    try {
      val q =
        s"""
           |DELETE FROM $tableName WHERE id = ? AND name = ? AND tag = ?
            """.stripMargin
      update(q, stencil.id, prevName, stencil.tag)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error deleting bucket [$stencil.id] ${e.getMessage}", e)
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
  def apply(tableName: String, historyTableName: String, bucketRegistryTable: String, jdbcHelper: TMySQLFactory) =
    new StencilDao(tableName: String, historyTableName: String, bucketRegistryTable: String, jdbcHelper: TMySQLFactory)
}
