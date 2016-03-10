package com.flipkart.connekt.commons.tests

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncManager
import com.flipkart.connekt.commons.tests.connections.MockConnectionProvider
import com.flipkart.connekt.commons.utils.ConfigUtils
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

    ConnektLogger(LogFile.SERVICE).info(s"Test config initializing, configServiceHost: $configServiceHost:$configServicePort")
    ConnektConfig(configServiceHost, configServicePort)(Seq("fk-connekt-root", "fk-connekt-".concat(ConfigUtils.getConfEnvironment), "fk-connekt-busybees-akka"))

    SyncManager.create(ConnektConfig.getString("sync.zookeeper").get)

    DaoFactory.setUpConnectionProvider(new MockConnectionProvider())

    val hConfig = ConnektConfig.getConfig("receptors.connections.hbase")

    DaoFactory.initHTableDaoFactory(hConfig.get)

    val mysqlConf = ConnektConfig.getConfig("receptors.connections.mysql").getOrElse(ConfigFactory.empty())
    DaoFactory.initMysqlTableDaoFactory(mysqlConf)

    val couchbaseCf = ConnektConfig.getConfig("receptors.connections.couchbase").getOrElse(ConfigFactory.empty())
    DaoFactory.initCouchbaseCluster(couchbaseCf)

    val specterConfig = ConnektConfig.getConfig("receptors.connections.specter").getOrElse(ConfigFactory.empty())
    DaoFactory.initSpecterSocket(specterConfig)

    val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    val kafkaProducerHelper = KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)

    ServiceFactory.initPNMessageService(DaoFactory.getPNRequestDao, DaoFactory.getUserConfigurationDao, kafkaProducerHelper, null)
    ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao, DaoFactory.getPNRequestDao, null)
    ServiceFactory.initAuthorisationService(DaoFactory.getPrivDao, DaoFactory.getUserInfoDao)
    ServiceFactory.initStorageService(DaoFactory.getKeyChainDao)

    ConnektLogger(LogFile.SERVICE).info("BaseReceptorsTest bootstrapped.")
  }
}
