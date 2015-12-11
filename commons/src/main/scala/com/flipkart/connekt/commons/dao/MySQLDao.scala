package com.flipkart.connekt.commons.dao

import java.sql.ResultSet
import javax.persistence.Column

import org.springframework.jdbc.core.{JdbcTemplate, RowMapper}

import scala.reflect.ClassTag

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
trait MySQLDao extends Dao {
  def update(statement: String, args: Object*)(implicit jdbcTemplate: JdbcTemplate): Int = {
    jdbcTemplate.update(statement, args: _*)
  }
  def query[T](statement: String, args: Object*)(implicit cTag: reflect.ClassTag[T], jdbcTemplate: JdbcTemplate): T = {
    jdbcTemplate.queryForObject(statement, getRowMapper[T], args:_*)
  }

  private def getRowMapper[T: ClassTag]: RowMapper[T] = {
    new RowMapper[T] {
      override def mapRow(rs: ResultSet, rowNum: Int): T = {
        create[T](rs)
      }
    }
  }

  def getDbColumnValues(rs: ResultSet): Map[String, Object] = {
    val rsMeta = rs.getMetaData
    var dbFieldValueMap = Map[String, Object]()

    for(i <- 1 to rsMeta.getColumnCount)
      dbFieldValueMap += rsMeta.getColumnLabel(i) -> rs.getObject(i)
    dbFieldValueMap
  }

  def create[T](rs: ResultSet)(implicit cTag: reflect.ClassTag[T]): T = {
    val instance: T = cTag.runtimeClass.newInstance().asInstanceOf[T]
    val dbFieldValueMap = getDbColumnValues(rs)

    instance.getClass.getDeclaredFields.foreach(f => {
      f.setAccessible(true)
      val dbColumnName = f.getAnnotation(classOf[Column]).name()
      if(f.getType == classOf[Enumeration#Value]) {
        getEnum[T](dbFieldValueMap(dbColumnName))
      } else {
        f.set(instance, dbFieldValueMap(dbColumnName))
      }
    })

    instance
  }

  def getEnum[T](enumVal: Object): Enumeration#Value = {
    val method = classOf[Enumeration].getMethod("withName", classOf[String])
    method.invoke(null, enumVal.asInstanceOf[String]).asInstanceOf[Enumeration#Value]
  }
}
