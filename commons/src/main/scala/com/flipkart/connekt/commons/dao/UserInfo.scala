package com.flipkart.connekt.commons.dao

import java.lang.Long

import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger, MySQLFactoryWrapper}
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}
import com.flipkart.connekt.commons.utils.StringUtils._

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

  override def addUserInfo(user: AppUser, updatedBy: String): Boolean = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |INSERT INTO $table(userId, apikey, groups, lastUpdatedTs, updatedBy) VALUES(?, ?, ?, ?, ?)
      """.stripMargin

    try {
      update(q, user.userId, user.apiKey, user.groups, new Long(System.currentTimeMillis()), updatedBy).equals(1)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error adding user [${user.getJson}] info: ${e.getMessage}", e)
        false
    }

  }
}
