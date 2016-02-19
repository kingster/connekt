package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.Key

/**
 * Created by nidhi.mehla on 17/02/16.
 */
trait TKeyChainDao {

  def get(key: String): Option[Key]

  def put(data: Key): Unit

  def getKeys(kind: String): List[Key]
}
