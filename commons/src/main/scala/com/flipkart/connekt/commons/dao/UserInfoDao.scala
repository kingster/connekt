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

import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{TMySQLFactory, ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

class UserInfoDao(table: String, mysqlFactory: TMySQLFactory) extends TUserInfo with MySQLDao {

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
         |INSERT INTO $table(userId, apikey, groups, updatedBy, contact) VALUES(?, ?, ?, ?, ?)
         |ON DUPLICATE KEY UPDATE groups = ?, updatedBy = ?, contact = ?
      """.stripMargin

    try {
      update(q, user.userId, user.apiKey, user.groups, user.updatedBy, user.contact, user.groups, user.updatedBy, user.contact)
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

  def apply(tableName: String = "USER_INFO", mysqlFactory: TMySQLFactory) =
    new UserInfoDao(tableName, mysqlFactory)


}
