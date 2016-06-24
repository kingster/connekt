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

import com.flipkart.connekt.commons.entities.Subscription
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, TMySQLFactory}
import com.flipkart.connekt.commons.utils.StringUtils._

class SubscriptionDao(subscriptionTable:String, jdbcHelper: TMySQLFactory) extends TSubscriptionDao with MySQLDao {

  val mySQLHelper = jdbcHelper

  override def add(subscription: Subscription): Unit = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val sql = s"""
                  |INSERT INTO $subscriptionTable (id, name, sink, createdBy, createdTS, lastUpdatedTS, eventFilter, shutdownThreshold) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                  |ON DUPLICATE KEY UPDATE  name = ?, sink = ?, lastUpdatedTS = ?, eventFilter = ?, shutdownThreshold = ?
        """.stripMargin
      update(sql,subscription.id, subscription.name, subscription.sink.getJson,subscription.createdBy, subscription.createdTS,
        subscription.lastUpdatedTS, subscription.eventFilter,subscription.shutdownThreshold,
        subscription.name, subscription.sink.getJson, subscription.lastUpdatedTS, subscription.eventFilter,subscription.shutdownThreshold)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error writing subscription [${subscription.id}] ${e.getMessage}", e)
        throw e
    }
  }

  override def get(id: String): Option[ Subscription ] = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val sql =
        s"""
           |SELECT * FROM $subscriptionTable WHERE id = ?
            """.stripMargin

      query[Subscription](sql, id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching subscription [$id] ${e.getMessage}", e)
        throw e
    }
  }

  override def delete(id: String): Unit = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val sql = s"""
                  |DELETE FROM $subscriptionTable WHERE id = ?
        """.stripMargin
      update(sql,id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error writing subscription [$id] ${e.getMessage}", e)
        throw e
    }
  }

}

object SubscriptionDao {
  def apply(subscriptionTable: String, jdbcHelper: TMySQLFactory) =
    new SubscriptionDao(subscriptionTable: String, jdbcHelper: TMySQLFactory)
}
