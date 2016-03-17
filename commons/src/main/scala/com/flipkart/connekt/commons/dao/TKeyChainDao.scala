/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.Key

trait TKeyChainDao {

  def get(key: String): Option[Key]

  def put(data: Key): Unit

  def getKeys(kind: String): List[Key]
}
