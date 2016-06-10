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

import java.lang.reflect.{Field, Modifier}
import java.sql.ResultSet
import javax.persistence.Column

import com.flipkart.connekt.commons.entities.EnumTypeHint
import org.springframework.jdbc.core.{JdbcTemplate, RowMapper}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import com.flipkart.connekt.commons.utils.StringUtils._

trait JSONField

trait MySQLDao extends Dao {
  def update(statement: String, args: Any*)(implicit jdbcTemplate: JdbcTemplate): Int = {
    jdbcTemplate.update(statement, args.map(_.asInstanceOf[Object]): _*)
  }
  def query[T](statement: String, args: Any*)(implicit cTag: reflect.ClassTag[T], jdbcTemplate: JdbcTemplate): Option[T] = {
    jdbcTemplate.query(statement, getRowMapper[T], args.map(_.asInstanceOf[Object]):_*).asScala.headOption
  }

  def queryForList[T](statement: String, args: Any*)(implicit cTag: reflect.ClassTag[T], jdbcTemplate: JdbcTemplate): List[T] = {
    jdbcTemplate.query(statement, getRowMapper[T], args.map(_.asInstanceOf[Object]):_*).asScala.toList
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
        val clz = getEnumObjectClass(f.getType.asInstanceOf[Class[Enumeration#Value]], f)
        val v = getEnum(dbFieldValueMap(dbColumnName), clz)
        f.set(instance, v)
      } else if (classOf[JSONField].isAssignableFrom(f.getType)){
        f.set(instance, dbFieldValueMap(dbColumnName).toString.getObj(f.getType))
      }
      else {
        f.set(instance, dbFieldValueMap(dbColumnName))
      }
    })

    instance
  }

  /**
   * Convert the value to an Enumeration.Value instance using class <tt>enumObjectClass</tt>'s
   * valueOf method. Returns an instance of <tt>Enumeration.Value</tt>.
   */
  private def getEnum[T](value: Any, enumObjectClass: Class[T]): Enumeration#Value = {
    if (Modifier.isAbstract(enumObjectClass.getModifiers)) {
      throw new IllegalArgumentException("cannot get type information for enum " + value)
    }
    val method = enumObjectClass.getMethod("withName", classOf[String])
    method.invoke(null, value.asInstanceOf[String]).asInstanceOf[Enumeration#Value]
  }



  private def getEnumObjectClass[T <: Enumeration#Value](targetClass: Class[T], y: Field): Class[_] = {
    val enumObjectClass = y.getAnnotation(classOf[EnumTypeHint]) match {
      case null =>
        targetClass.getEnclosingClass
      case an => Class.forName(an.value)
    }
    enumObjectClass
  }
}
