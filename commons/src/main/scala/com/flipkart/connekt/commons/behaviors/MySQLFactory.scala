package com.flipkart.connekt.commons.behaviors

import org.springframework.jdbc.core.JdbcTemplate

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
trait MySQLFactory {
  def getJDBCInterface: JdbcTemplate
}
