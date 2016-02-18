package com.flipkart.connekt.commons.tests

import com.flipkart.connekt.commons.connections.ConnectionProvider
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}

/**
 * @author aman.shrivastava on 10/12/15.
 */
class CommonsBaseTest extends ConnektUTSpec {

  override def beforeAll() = {
    super.beforeAll()
    bootstrapReceptors()
  }

  private def bootstrapReceptors() = {

    ConnektConfig(configHost = "10.47.0.101", configPort = 80)()

    //DaoFactory.setUpConnectionProvider(new MockConnectionProvider)
    DaoFactory.setUpConnectionProvider(new ConnectionProvider)

    val hConfig = ConnektConfig.getConfig("receptors.connections.hbase").getOrElse(ConfigFactory.empty())
    DaoFactory.initHTableDaoFactory(hConfig)

    val mysqlConf = ConnektConfig.getConfig("receptors.connections.mysql").getOrElse(ConfigFactory.empty())
    DaoFactory.initMysqlTableDaoFactory(mysqlConf)

    val couchbaseCf = ConnektConfig.getConfig("receptors.connections.couchbase").getOrElse(ConfigFactory.empty())
    DaoFactory.initCouchbaseCluster(couchbaseCf) // Mocked

    val _specterConfig = ConnektConfig.getConfig("receptors.connections.specter").getOrElse(ConfigFactory.empty())
    val specterConfig = _specterConfig.withValue("lib.path", ConfigValueFactory.fromAnyRef("build/fk-pf-connekt/deploy/opt/newsclub/lib-native"))
    DaoFactory.initSpecterSocket(specterConfig)

    val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)

    ServiceFactory.initMessageService(DaoFactory.getRequestInfoDao, KafkaProducerHelper, null)
    ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao, DaoFactory.getRequestInfoDao, null)
    ServiceFactory.initAuthorisationService(DaoFactory.getPrivDao, DaoFactory.getUserInfoDao)
    ServiceFactory.initStorageService(DaoFactory.getStorageDao)


    ConnektLogger(LogFile.SERVICE).info("BaseReceptorsTest bootstrapped.")
  }
}
