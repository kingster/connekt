package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.ResourcePriv
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, MySQLFactoryWrapper}
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

/**
 * @author aman.shrivastava on 11/12/15.
 */
class PrivDao(table: String, mysqlFactory: MySQLFactoryWrapper) extends TPrivDao with MySQLDao {
  val mysqlHelper = mysqlFactory

  override def getPrivileges(identifier: String, _type: String): Option[ResourcePriv] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * from $table where userType = ? and userId = ?
      """.stripMargin

    try {
      Some(query[ResourcePriv](q, _type, identifier))
    } catch {
      case e@(_: IncorrectResultSizeDataAccessException | _: DataAccessException) =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching user [$identifier] info: ${e.getMessage}", e)
        None
    }
  }

  override def setPrivileges(identifier: String, _type: String, resources: String): Boolean = ???
}
