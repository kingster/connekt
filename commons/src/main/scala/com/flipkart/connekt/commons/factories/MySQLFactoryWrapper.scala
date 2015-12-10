package com.flipkart.connekt.commons.factories

import java.util.Properties
import javax.sql.DataSource

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.typesafe.config.Config
import org.apache.commons.dbcp2.BasicDataSourceFactory
import org.springframework.jdbc.core.JdbcTemplate

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
class MySQLFactoryWrapper private(dataSource: DataSource) extends MySQLFactory {
  var source: DataSource = dataSource

  def this(host: String, database: String, username: String, password: String, poolProps: Config) = {
    this(
      BasicDataSourceFactory.createDataSource(PropsHelper.getConnProperties(host, database, username, password, poolProps))
    )
  }

  override def getJDBCInterface: JdbcTemplate = new JdbcTemplate(source)
}

object PropsHelper {
  val connUrl = "jdbc:mysql://%s/%s?autoReconnect=true&useUnicode=true&characterEncoding=utf-8"
  val driverClassName = "com.mysql.jdbc.Driver"

  def getConnProperties(host: String, database: String, username: String, password: String, poolProps: Config): Properties = {
    val connProps = new Properties()
    connProps.setProperty("url", connUrl.format(host, database))
    connProps.setProperty("driverClassName", driverClassName)
    connProps.setProperty("username", username)
    connProps.setProperty("password", password)
    connProps.setProperty("maxIdle", poolProps.getString("maxIdle"))
    connProps.setProperty("initialSize", poolProps.getString("initialSize"))
    connProps.setProperty("maxActive", poolProps.getString("maxActive"))
    connProps.setProperty("autoReconnect", "true")
    connProps.setProperty("testOnBorrow", "true")
    connProps.setProperty("testOnReturn", "false")
    connProps.setProperty("validationQuery", "select 1")
    connProps.setProperty("validationQueryTimeout", "2000")
    connProps
  }
}