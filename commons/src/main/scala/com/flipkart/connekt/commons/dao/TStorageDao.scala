package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.DataStore

/**
 * Created by nidhi.mehla on 17/02/16.
 */
trait TStorageDao {

  def get(key: String): Option[DataStore]

  def put(data: DataStore): Unit

}
