package com.flipkart.connekt.commons.dao

import org.springframework.jdbc.core.JdbcTemplate

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
trait MySQLDao extends Dao {
  def update(statement: String, args: Any*)(implicit jdbcTemplate: JdbcTemplate): Int = {
    jdbcTemplate.update(statement, args)
  }
  def query[T](statement: String, args: Any*)(implicit cTag: reflect.ClassTag[T], jdbcTemplate: JdbcTemplate): T = {
    jdbcTemplate.queryForObject(statement, cTag.runtimeClass.asInstanceOf[Class[T]])
  }
}
