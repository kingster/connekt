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

import com.flipkart.connekt.commons.entities.Key
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, TMySQLFactory}
import org.springframework.dao.DataAccessException

class KeyChainDao(table: String, mysqlFactory: TMySQLFactory) extends TKeyChainDao with MySQLDao {
  val mysqlHelper = mysqlFactory

  override def get(key: String): Option[Key] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table WHERE `key` = ?
      """.stripMargin
    try {
      query[Key](q, key)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error in getting $key", e)
        throw e
    }
  }



  override def put(data: Key): Unit = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |INSERT INTO $table(`key`, `kind`, `value`, `creationTS`, `lastUpdatedTS`, `expireTS`) VALUES(?, ?, ?, ?, ?, ?)
         |ON DUPLICATE KEY UPDATE value = ?, lastUpdatedTS = ?, expireTS = ?
      """.stripMargin
    try {
      update(q, data.keyName, data.kind, data.value, data.creationTS, data.lastUpdatedTS, data.expireTS, data.value, data.lastUpdatedTS, data.expireTS)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error adding data to data store for [${data.keyName}] info: ${e.getMessage}", e)
        throw e
    }
  }

  override def getKeys(kind: String): List[Key] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table where `kind`= ?
       """.stripMargin
    try {
      queryForList[Key](q, kind)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching keys of kind [$kind] error: ${e.getMessage}", e)
        throw e
    }
  }
}

object KeyChainDao {
  def apply(tableName: String = "DATA_STORE", mysqlFactory: TMySQLFactory) =
    new KeyChainDao(tableName, mysqlFactory)
}
