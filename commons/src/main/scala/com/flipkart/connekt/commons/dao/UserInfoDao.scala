/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

class UserInfoDao(table: String, mysqlFactory: MySQLFactory) extends TUserInfo with MySQLDao {

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
         |INSERT INTO $table(userId, apikey, groups, updatedBy) VALUES(?, ?, ?, ?)
         |ON DUPLICATE KEY UPDATE apikey = ?, groups = ?, updatedBy = ?
      """.stripMargin

    try {
      update(q, user.userId, user.apiKey, user.groups, user.updatedBy, user.apiKey, user.groups, user.updatedBy)
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

object UserInfoDao {

  def apply(tableName: String = "USER_INFO", mysqlFactory: MySQLFactory) =
    new UserInfoDao(tableName, mysqlFactory)


}
