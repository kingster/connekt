package com.flipkart.connekt.commons.dao


import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.entities.DataStore
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import org.springframework.dao.DataAccessException

/**
 * Created by nidhi.mehla on 17/02/16.
 */
class StorageDao(table: String, mysqlFactory: MySQLFactory) extends TStorageDao with MySQLDao {
  val mysqlHelper = mysqlFactory

  override def get(key: String): Option[DataStore] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table WHERE `key` = ?
      """.stripMargin
    try {
      query[DataStore](q, key)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error in getting $key", e)
        throw e
    }
  }

  override def put(data: DataStore): Unit = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |INSERT INTO $table(`key`, `type`, `value`, `creationTS`, `lastUpdatedTS`) VALUES(?, ?, ?, ?, ?)
         |ON DUPLICATE KEY UPDATE value = ?, lastUpdatedTS = ?
      """.stripMargin
    try {
      update(q, data.key, data.`type`, data.value, data.creationTS, data.lastUpdatedTS, data.value, data.lastUpdatedTS)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error adding data to data store for [${data.key}] info: ${e.getMessage}", e)
        throw e
    }
  }
}

object StorageDao {
  def apply(tableName: String = "DATA_STORE", mysqlFactory: MySQLFactory) =
    new StorageDao(tableName, mysqlFactory)
}
