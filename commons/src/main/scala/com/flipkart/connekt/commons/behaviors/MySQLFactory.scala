/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.behaviors

import org.springframework.jdbc.core.JdbcTemplate

trait MySQLFactory {
  def getJDBCInterface: JdbcTemplate
}
