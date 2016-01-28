package com.flipkart.connekt.receptors.routes

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.{CommonsBaseTest, ConnektUTSpec}
import com.flipkart.connekt.commons.tests.connections.MockConnectionProvider
import com.typesafe.config.ConfigFactory

/**
 * @author aman.shrivastava on 10/12/15.
 */
abstract class BaseReceptorsTest extends CommonsBaseTest {

  override def beforeAll() = {
    super.beforeAll()
  }

}
