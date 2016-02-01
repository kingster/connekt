package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.entities.{Bucket, Stencil}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
class StencilDao(tableName: String, historyTableName: String, bucketRegistryTable: String, jdbcHelper: MySQLFactory) extends TStencilDao with MySQLDao {
  val mysqlHelper = jdbcHelper

  override def getStencil(id: String, version: Option[String] = None): Option[Stencil] = {
    implicit val j = mysqlHelper.getJDBCInterface
    try {
      version match {
        case Some(ver) =>
          val q =
            s"""
               |SELECT * FROM $historyTableName WHERE id = ? and version = ?
            """.stripMargin
          query[Stencil](q, id, ver)
        case _ =>
          val q =
            s"""
               |SELECT * FROM $tableName WHERE id = ?
            """.stripMargin
          query[Stencil](q, id)
      }
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching stencil [$id] ${e.getMessage}", e)
        throw e
    }
  }

  override def writeStencil(stencil: Stencil): Unit = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q1 =
      s"""
         |INSERT INTO $tableName (id, engine, engineFabric, createdBy, updatedBy, version, creationTS, lastUpdatedTS, bucket) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
         |ON DUPLICATE KEY UPDATE engine = ?, engineFabric = ?, updatedBy = ?, version = version + 1, bucket = ?
      """.stripMargin

    val q2 =
      s"""
        |INSERT INTO $historyTableName (id, engine, engineFabric, createdBy, updatedBy, version, creationTS, lastUpdatedTS, bucket) VALUES(?, ?, ?, ?, ?, (SELECT VERSION from $tableName where id = ?), ?, ?, ?)
      """.stripMargin

    try {
      update(q1, stencil.id, stencil.engine.toString, stencil.engineFabric, stencil.createdBy, stencil.updatedBy, stencil.version.toString, stencil.creationTS, stencil.creationTS, stencil.bucket, stencil.engine.toString, stencil.engineFabric, stencil.updatedBy, stencil.bucket)
      update(q2, stencil.id, stencil.engine.toString, stencil.engineFabric, stencil.createdBy, stencil.updatedBy, stencil.id, stencil.creationTS, stencil.creationTS, stencil.bucket)
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
  def apply(tableName: String, historyTableName: String, bucketRegistryTable: String,jdbcHelper: MySQLFactory) =
    new StencilDao(tableName: String, historyTableName: String, bucketRegistryTable: String,jdbcHelper: MySQLFactory)
}
