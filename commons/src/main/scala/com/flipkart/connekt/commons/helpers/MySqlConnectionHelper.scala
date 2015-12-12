package com.flipkart.connekt.commons.helpers

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.factories.MySQLFactoryWrapper
import com.typesafe.config.Config

/**
 * @author aman.shrivastava on 13/12/15.
 */
object MySqlConnectionHelper {
  def createMySqlConnection(mysqlConnectionConfig: Config): MySQLFactory = {
    val host = mysqlConnectionConfig.getString("host")
    val database = mysqlConnectionConfig.getString("database")
    val username = mysqlConnectionConfig.getString("username")
    val password = mysqlConnectionConfig.getString("password")
    val poolProps = mysqlConnectionConfig.getConfig("poolProps")

    new MySQLFactoryWrapper(host, database, username, password, poolProps)
  }
}
