package com.flipkart.connekt.commons.tests

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.connections.MockConnectionProvider
import com.typesafe.config.ConfigFactory

/**
 * @author aman.shrivastava on 10/12/15.
 */
class CommonsBaseTest extends ConnektUTSpec {

  override def beforeAll() = {
    super.beforeAll()
    bootstrapReceptors()
  }

  private def bootstrapReceptors() = {

    ConnektConfig(configHost = "config-service.nm.flipkart.com", configPort = 80, configAppVersion = 1)()

    DaoFactory.setUpConnectionProvider(new MockConnectionProvider)

    val hConfig = ConnektConfig.getConfig("receptors.connections.hbase").getOrElse(ConfigFactory.empty())
    DaoFactory.initHTableDaoFactory(hConfig)

    val mysqlConf = ConnektConfig.getConfig("receptors.connections.mysql").getOrElse(ConfigFactory.empty())
    DaoFactory.initMysqlTableDaoFactory(mysqlConf)

    //Setup tables.
    DaoFactory.mysqlFactoryWrapper.getJDBCInterface.execute(
      """
        |CREATE SCHEMA IF NOT EXISTS `connekt`;
        |CREATE TABLE IF NOT EXISTS `USER_INFO` (`userId` varchar(100) NOT NULL DEFAULT '',`apikey` varchar(100) NOT NULL DEFAULT '',`groups` text,`lastUpdatedTS` bigint(20) UNSIGNED NOT NULL,`updatedBy` varchar(100) NOT NULL DEFAULT '',PRIMARY KEY (`userId`),UNIQUE KEY APIKEY_0 (`apikey`));
        |CREATE TABLE IF NOT EXISTS `RESOURCE_PRIV` (`userId` varchar(100) NOT NULL DEFAULT '',`userType` varchar(40) DEFAULT 'USER',`resources` text,PRIMARY KEY (`userId`));
        |CREATE TABLE IF NOT EXISTS `STENCIL_STORE` (`id` varchar(40) NOT NULL DEFAULT '',`engine` varchar(40) NOT NULL DEFAULT 'VELOCITY',`engineFabric` text NOT NULL,`createdBy` varchar(45) NOT NULL DEFAULT '',`updatedBy` varchar(45) NOT NULL DEFAULT '',`version` int(11) NOT NULL,`creationTS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,`lastUpdatedTS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY (`id`,`version`));
        |
      """.stripMargin)


    val couchbaseCf = ConnektConfig.getConfig("receptors.connections.couchbase").getOrElse(ConfigFactory.empty())
    DaoFactory.initCouchbaseCluster(couchbaseCf) // Mocked

    val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)

    ServiceFactory.initMessageService(DaoFactory.getRequestInfoDao, KafkaProducerHelper, null)
    ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao, DaoFactory.getRequestInfoDao, null)
    ServiceFactory.initAuthorisationService(DaoFactory.getPrivDao, DaoFactory.getUserInfoDao)


    ConnektLogger(LogFile.SERVICE).info("BaseReceptorsTest bootstrapped.")
  }
}
