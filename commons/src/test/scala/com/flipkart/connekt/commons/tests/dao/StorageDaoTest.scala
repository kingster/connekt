package com.flipkart.connekt.commons.tests.dao

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DataStore
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * Created by nidhi.mehla on 17/02/16.
 */
class StorageDaoTest extends CommonsBaseTest {

  val id = UUID.randomUUID().toString
  val data = new DataStore(UUID.randomUUID().toString.take(6), "STRING", "dataValue".getBytes, new Date(), new Date())

  "StorageDao test" should "add random data" in {
    val storageDao = DaoFactory.getStorageDao
    noException should be thrownBy storageDao.put(data)
  }

  "Storage test" should "get stored data" in {
    val storageDao = DaoFactory.getStorageDao
    noException should be thrownBy storageDao.get(data.key)
    new String(storageDao.get(data.key).get.value) shouldEqual "dataValue"
  }
}
