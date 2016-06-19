package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.Subscription
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, TMySQLFactory}
import com.flipkart.connekt.commons.utils.StringUtils._

/**
  * Created by harshit.sinha on 08/06/16.
  */

class SubscriptionDao(subscriptionTable:String, jdbcHelper: TMySQLFactory) extends TSubscriptionDao with MySQLDao {

  val mySQLHelper = jdbcHelper

  override def add(subscription: Subscription): Boolean = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val q1 = s"""
                  |INSERT INTO $subscriptionTable (id, name, relayPoint, createdBy, createdTS, lastUpdatedTS, groovyFilter, shutdownThreshold) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                  |ON DUPLICATE KEY UPDATE  name = ?, relayPoint = ?, lastUpdatedTS = ?, groovyFilter = ?, shutdownThreshold = ?
        """.stripMargin
      update(q1,subscription.id, subscription.name, subscription.relayPoint.getJson,subscription.createdBy, subscription.createdTS,
        subscription.lastUpdatedTS, subscription.groovyFilter,subscription.shutdownThreshold,
        subscription.name, subscription.relayPoint.getJson, subscription.lastUpdatedTS, subscription.groovyFilter,subscription.shutdownThreshold)
      true
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error writing subscription [${subscription.id}] ${e.getMessage}", e)
        throw e
    }
  }

  override def get(id: String): Option[ Subscription ] = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val q1 =
        s"""
           |SELECT * FROM $subscriptionTable WHERE id = ?
            """.stripMargin

      query[Subscription](q1, id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching subscription [$id] ${e.getMessage}", e)
        throw e
    }
  }

  override def delete(id: String): Boolean = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val q1 = s"""
                  |DELETE FROM $subscriptionTable WHERE id = ?
        """.stripMargin
      update(q1,id)
      true
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
