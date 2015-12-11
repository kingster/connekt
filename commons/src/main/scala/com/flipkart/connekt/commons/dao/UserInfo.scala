package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, MySQLFactoryWrapper}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

/**
 *
 *
 * @author durga.s
 * @version 12/11/15
 */
class UserInfo(table: String, mysqlFactory: MySQLFactoryWrapper) extends TUserInfo with MySQLDao {
  val mysqlHelper = mysqlFactory

  override def getUserInfo(userId: String): Option[AppUser] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table WHERE userId = ?
      """.stripMargin

    try {
      Some(query[AppUser](q, userId))
    } catch {
      case e@(_: IncorrectResultSizeDataAccessException | _: DataAccessException) =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching user [$userId] info: ${e.getMessage}", e)
        None
    }

  }

  override def addUserInfo(user: AppUser): Boolean = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |INSERT INTO $table(userId, apikey, groups, lastUpdatedTs, updatedBy) VALUES(?, ?, ?, ?, ?)
      """.stripMargin

    try {
      update(q, user.userId, user.apiKey, user.groups,  new java.lang.Long(System.currentTimeMillis()), user.updatedBy).equals(1)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error adding user [${user.getJson}] info: ${e.getMessage}", e)
        false
    }

  }
}
