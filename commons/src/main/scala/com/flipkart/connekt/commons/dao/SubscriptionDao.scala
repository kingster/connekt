package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.{Subscription}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, TMySQLFactory}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.Try

/**
  * Created by harshit.sinha on 08/06/16.
  */

class SubscriptionDao(subscriptionTable:String, jdbcHelper: TMySQLFactory) extends TSubscriptionDao with MySQLDao {

  val mySQLHelper = jdbcHelper

  override def getSubscription(sId: String, createdBy: String): Option[ Subscription ] = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val q1 =
        s"""
           |SELECT * FROM $subscriptionTable WHERE sId = ? AND createdBy = ?
            """.stripMargin

      query[Subscription](q1, sId, createdBy)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching subscription [$sId] ${e.getMessage}", e)
        throw e
    }
  }


  override def writeSubscription(subscription: Subscription): Option[Subscription] = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val q1 = s"""
         |INSERT INTO $subscriptionTable (sId, sName, endpoint, createdBy, createdTS, lastUpdatedTS, groovyString, shutdownThreshold) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
         |ON DUPLICATE KEY UPDATE  endpoint = ?, lastUpdatedTS = ?, groovyString = ?, shutdownThreshold = ?
        """.stripMargin
       update(q1,subscription.sId, subscription.sName, subscription.endpoint.getJson,subscription.createdBy, subscription.createdTS,
         subscription.lastUpdatedTS, subscription.groovyString,subscription.shutdownThreshold,
         subscription.endpoint.getJson, subscription.lastUpdatedTS, subscription.groovyString,subscription.shutdownThreshold)
      Option(subscription)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error writing subscription [${subscription.sId}] ${e.getMessage}", e)
        throw e
    }
  }


  override def deleteSubscription(sId: String): Boolean = {
    implicit val j = mySQLHelper.getJDBCInterface
    try {
      val q1 = s"""
                  |DELETE FROM $subscriptionTable WHERE sId = ?
        """.stripMargin
      update(q1,sId)
      true
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.DAO).error(s"Error writing subscription [$sId] ${e.getMessage}", e)
        throw e
    }
  }

}

object SubscriptionDao {
  def apply(subscriptionTable: String, jdbcHelper: TMySQLFactory) =
    new SubscriptionDao(subscriptionTable: String, jdbcHelper: TMySQLFactory)
}
