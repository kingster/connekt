package com.flipkart.connekt.receptors.routes

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.typesafe.config.ConfigFactory

/**
 * @author aman.shrivastava on 10/12/15.
 */
abstract class BaseReceptorsTest extends ConnektUTSpec {
  override def beforeAll() = {
    super.beforeAll()
    bootstrapReceptors()
  }

  private def bootstrapReceptors() = {
    ConnektConfig(configHost = "config-service.nm.flipkart.com", configPort = 80, configAppVersion = 1)

    val hConfig = ConnektConfig.getConfig("receptors.connections.hbase")
    DaoFactory.initHTableDaoFactory(hConfig.get)

    val mysqlConf = ConnektConfig.getConfig("receptors.connections.mysql").getOrElse(ConfigFactory.empty())
    DaoFactory.initMysqlTableDaoFactory(mysqlConf)


    val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)

    ServiceFactory.initMessageService(DaoFactory.getRequestInfoDao, KafkaProducerHelper, null)
    ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao, DaoFactory.getRequestInfoDao, null)
    ServiceFactory.initAuthorisationService(DaoFactory.getPrivDao, DaoFactory.getUserInfoDao)


    ConnektLogger(LogFile.SERVICE).info("BaseReceptorsTest bootstrapped.")
  }
}
