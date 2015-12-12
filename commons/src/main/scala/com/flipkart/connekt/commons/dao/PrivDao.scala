package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.MySQLFactory
import com.flipkart.connekt.commons.dao.PrivDao.Operation
import com.flipkart.connekt.commons.dao.PrivDao.Operation.Operation
import com.flipkart.connekt.commons.entities.ResourcePriv
import com.flipkart.connekt.commons.entities.UserType.UserType
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import org.springframework.dao.{DataAccessException, IncorrectResultSizeDataAccessException}

/**
 * @author aman.shrivastava on 11/12/15.
 */
class PrivDao(table: String, mysqlFactory: MySQLFactory) extends TPrivDao with MySQLDao {

  val mysqlHelper = mysqlFactory

  override def getPrivileges(identifier: String, _type: UserType): Option[ResourcePriv] = {
    implicit val j = mysqlHelper.getJDBCInterface
    val q =
      s"""
         |SELECT * from $table where userType = ? and userId = ?
      """.stripMargin

    try {
      query[ResourcePriv](q, _type.toString, identifier)
    } catch {
      case e@(_: IncorrectResultSizeDataAccessException | _: DataAccessException) =>
        ConnektLogger(LogFile.DAO).error(s"Error fetching user [$identifier] info: ${e.getMessage}", e)
        throw e
    }
  }

  override def addPrivileges(identifier: String, _type: UserType, privs: List[String]) = {
    modifyPrivileges(identifier,_type, privs, Operation.ADD)
  }

  override def removePrivileges(identifier: String, _type: UserType, privs: List[String]) = {
    modifyPrivileges(identifier,_type, privs, Operation.DELETE)

  }

  private def modifyPrivileges(identifier: String, _type: UserType, privs: List[String], operation:Operation): Unit ={

    implicit val jdbc = mysqlHelper.getJDBCInterface
    jdbc.execute("START TRANSACTION")
    val sql = s"SELECT * from $table where userType = ? and userId = ? FOR UPDATE"
    val record = query[ResourcePriv](sql, _type.toString, identifier)
    record match {
      case Some(userRecord) =>
        val sql = s"UPDATE $table set resources = ? WHERE userType = ? and userId = ? "
        val finalPrivileges:Set[String] = operation match {
          case Operation.ADD =>
            (userRecord.resources.split(',') ++ privs).toSet
          case Operation.DELETE =>
            (userRecord.resources.split(',') diff privs).toSet
        }
        try {
          update(sql, finalPrivileges.mkString(","), _type.toString, identifier)
        } catch {
          case e: DataAccessException =>
            ConnektLogger(LogFile.DAO).error(s"Error updating user [$identifier}] info: ${e.getMessage}", e)
            jdbc.execute("ROLLBACK")
            throw e
        }

      case None =>
        val sql = s"INSERT INTO $table(userId, userType, resources) VALUES(?, ?, ?)"
        try {
          update(sql, identifier, _type.toString, privs.mkString(","))
        } catch {
          case e: DataAccessException =>
            ConnektLogger(LogFile.DAO).error(s"Error adding user [$identifier}] info: ${e.getMessage}", e)
            jdbc.execute("ROLLBACK")
            throw e
        }

    }
    jdbc.execute("COMMIT")
  }




}

object PrivDao {

  def apply(tableName: String = "RESOURCE_PRIV", mysqlFactory: MySQLFactory) =
    new PrivDao(tableName, mysqlFactory)

  object Operation extends Enumeration{
    type Operation = Value
    val ADD, DELETE = Value
  }


}
