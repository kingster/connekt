package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

/**
 *
 *
 * @author durga.s
 * @version 12/11/15
 */
class UserInfo(table: String, mysqlFactory: MySQLFactory) extends TUserInfo with MySQLDao {
  val mysqlHelper = mysqlFactory

  override def getUserInfo(userId: String): Option[AppUser] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table WHERE userId = ?
      """.stripMargin

    try {
      query[AppUser](q, userId)
    } catch {
      case e @ (_: IncorrectResultSizeDataAccessException | _: DataAccessException) =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching user [$userId] info: ${e.getMessage}", e)
        throw e
    }

  }

  override def addUserInfo(user: AppUser) = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |INSERT INTO $table(userId, apikey, groups, lastUpdatedTs, updatedBy) VALUES(?, ?, ?, ?, ?)
         |ON DUPLICATE KEY UPDATE apikey = ?, groups = ?, lastUpdatedTs = ?, updatedBy = ?
      """.stripMargin

    try {
      update(q, user.userId, user.apiKey, user.groups,  new java.lang.Long(System.currentTimeMillis()), user.updatedBy, user.apiKey, user.groups, new java.lang.Long(System.currentTimeMillis()), user.updatedBy)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error adding user [${user.getJson}] info: ${e.getMessage}", e)
        throw e
    }

  }

  override def getUserByKey(key: String): Option[AppUser] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table WHERE apiKey = ?
      """.stripMargin

    try {
      query[AppUser](q, key)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error in getting $key", e)
        throw e
    }
  }
}

object UserInfo {

  def apply(tableName: String = "USER_INFO", mysqlFactory: MySQLFactory) =
    new UserInfo(tableName, mysqlFactory)


}
