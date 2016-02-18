package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * Created by nidhi.mehla on 17/02/16.
 */
class StorageServiceTest extends CommonsBaseTest {

  val keyData = UUID.randomUUID().toString.take(6)
  val keyValue = UUID.randomUUID().toString.take(6)

  "Storage Service" should "store key value" in {
    noException should be thrownBy ServiceFactory.getStorageService.put(keyData, keyValue)
  }

  "Storage Service" should "get value for key" in {
    noException should be thrownBy ServiceFactory.getStorageService.get(keyData)
    ServiceFactory.getStorageService.get(keyData).get shouldEqual keyValue.getBytes
  }

}
