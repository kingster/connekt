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

import com.flipkart.connekt.commons.entities.AppUserConfiguration
import com.flipkart.connekt.commons.entities.Channel.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, TMySQLFactory}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

class UserConfigurationDao(table: String, mysqlFactory: TMySQLFactory) extends TUserConfiguration with MySQLDao {

  val mysqlHelper = mysqlFactory

  override def getUserConfiguration(userId: String , channel : Channel): Option[AppUserConfiguration] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table WHERE userId = ? AND channel = ?
      """.stripMargin

    try {
      query[AppUserConfiguration](q, userId, channel.toString)
    } catch {
      case e @ (_: IncorrectResultSizeDataAccessException | _: DataAccessException) =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching UserConfiguration [$userId] info: ${e.getMessage}", e)
        throw e
    }

  }

  override def addUserConfiguration(ucfg: AppUserConfiguration) = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |INSERT INTO $table(userId, channel, platforms, queueName, maxRate, errorThresholdRate) VALUES(?, ?, ?, ?, ? , ?)
         |ON DUPLICATE KEY UPDATE  maxRate = ?, errorThresholdRate = ?, platforms = ?
      """.stripMargin

    try {
      update(q, ucfg.userId, ucfg.channel.toString, ucfg.queueName, ucfg.maxRate, ucfg.errorThresholdRate, ucfg.maxRate, ucfg.errorThresholdRate)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error adding UserConfiguration [${ucfg.getJson}] info: ${e.getMessage}", e)
        throw e
    }
  }

  override def getAllUserConfiguration(channel : Channel): List[AppUserConfiguration] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * FROM $table WHERE channel = ?
      """.stripMargin

    try {
      queryForList[AppUserConfiguration](q, channel.toString)
    } catch {
      case e @ (_: IncorrectResultSizeDataAccessException | _: DataAccessException) =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching allUserConfiguration for channel [${channel.toString}}] info: ${e.getMessage}", e)
        throw e
    }
  }
}

object UserConfigurationDao {

  def apply(tableName: String = "USER_CONFIG", mysqlFactory: TMySQLFactory) =
    new UserConfigurationDao(tableName, mysqlFactory)

}
