package com.flipkart.connekt.commons.services

import java.util.Date

import com.flipkart.connekt.commons.dao.TStorageDao
import com.flipkart.connekt.commons.entities.DataStore
import com.flipkart.connekt.commons.metrics.Instrumented


/**
 * Created by nidhi.mehla on 17/02/16.
 */
class StorageService(dao: TStorageDao) extends TStorageService with Instrumented {

  //  def put[T: TypeTag](key: String, value: T) = {
  //    val dataBytes: Array[Byte] = typeOf[T] match {
  //      case t if t =:= typeOf[String] => value.asInstanceOf[String].getBytes
  //      case t if t <:< typeOf[Array[Byte]] => value.asInstanceOf[Array[Byte]]
  //    }
  //    DaoFactory.getStorageDao.put(new DataStore(key, "STRING", dataBytes, new Date(), new Date()))
  //  }

  override def put(key: String, value: String): Boolean = {
    dao.put(new DataStore(key, "STRING", value.getBytes, new Date(), new Date()))
  }

  override def put(key: String, value: Array[Byte]): Boolean = {
    dao.put(new DataStore(key, "BYTE_ARRAY", value, new Date(), new Date()))
  }

  override def get(key: String): Option[Array[Byte]] = {
    dao.get(key).map(_.value)
  }

}
