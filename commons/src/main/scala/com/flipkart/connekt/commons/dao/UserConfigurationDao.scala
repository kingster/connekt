package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.entities.AppUserConfiguration
import com.flipkart.connekt.commons.entities.Channel.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

class UserConfigurationDao(table: String, mysqlFactory: MySQLFactory) extends TUserConfiguration with MySQLDao {

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
         |INSERT INTO $table(userId, channel, queueName, maxRate, errorThresholdRate) VALUES(?, ?, ?, ? , ?)
         |ON DUPLICATE KEY UPDATE  maxRate = ?, errorThresholdRate = ?
      """.stripMargin

    try {
      update(q, ucfg.userId, ucfg.channel.toString, ucfg.queueName, ucfg.maxRate.toString, ucfg.errorThresholdRate.toString, ucfg.maxRate.toString, ucfg.errorThresholdRate.toString)
    } catch {
      case e: DataAccessException =>
        ConnektLogger(LogFile.DAO).error(s"Error adding UserConfiguration [${ucfg.getJson}] info: ${e.getMessage}", e)
        throw e
    }

  }

}

object UserConfigurationDao {

  def apply(tableName: String = "USER_CONFIG", mysqlFactory: MySQLFactory) =
    new UserConfigurationDao(tableName, mysqlFactory)

}
