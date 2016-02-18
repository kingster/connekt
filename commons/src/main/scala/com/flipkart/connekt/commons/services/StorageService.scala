package com.flipkart.connekt.commons.services

import java.util.Date

import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.TStorageDao
import com.flipkart.connekt.commons.entities.DataStore
import com.flipkart.connekt.commons.metrics.Instrumented

import scala.util.Try


/**
 * Created by nidhi.mehla on 17/02/16.
 */
class StorageService(dao: TStorageDao) extends TStorageService with Instrumented {

  override def put(key: String, value: String): Try[Unit] = Try_ {
    dao.put(new DataStore(key, "STRING", value.getBytes, new Date(), new Date()))
  }

  override def put(key: String, value: Array[Byte]): Try[Unit] = Try_ {
    dao.put(new DataStore(key, "BYTE_ARRAY", value, new Date(), new Date()))
  }

  override def get(key: String): Try[Option[Array[Byte]]] = Try_ {
    dao.get(key).map(_.value)
  }

}
