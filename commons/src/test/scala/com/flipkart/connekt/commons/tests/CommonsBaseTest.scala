package com.flipkart.connekt.commons.tests

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.{KafkaConsumerHelper, KafkaProducerHelper}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncManager
import com.flipkart.connekt.commons.tests.connections.MockConnectionProvider
import com.flipkart.connekt.commons.utils.ConfigUtils
import com.typesafe.config.ConfigFactory

/**
 * @author aman.shrivastava on 10/12/15.
 */
class CommonsBaseTest extends ConnektUTSpec {

  val kafkaProducerHelper: Option[KafkaProducerHelper] = None
  val kafkaConsumerHelper: Option[KafkaConsumerHelper] = None

  override def beforeAll() = {
    super.beforeAll()
    bootstrapReceptors()
  }

  def getKafkaConsumerHelper = kafkaConsumerHelper.getOrElse({
    val kafkaConsumerConf = ConnektConfig.getConfig("connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaConsumerPoolConf = ConnektConfig.getConfig("connections.kafka.consumerPool").getOrElse(ConfigFactory.empty())
    ConnektLogger(LogFile.SERVICE).info(s"Kafka Conf: ${kafkaConsumerConf.toString}")
    KafkaConsumerHelper(kafkaConsumerConf, kafkaConsumerPoolConf)
  })

  def getKafkaProducerHelper = kafkaProducerHelper.getOrElse({
    val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)
  })

  private def bootstrapReceptors() = {

    ConnektLogger(LogFile.SERVICE).info(s"Test config initializing, configServiceHost: $configServiceHost:$configServicePort")
    ConnektConfig(configServiceHost, configServicePort)(Seq("fk-connekt-root", "fk-connekt-".concat(ConfigUtils.getConfEnvironment), "fk-connekt-receptors", "fk-connekt-busybees", "fk-connekt-busybees-akka"))
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

    ServiceFactory.initPNMessageService(DaoFactory.getPNRequestDao, DaoFactory.getUserConfigurationDao, getKafkaProducerHelper, null)
    ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao, DaoFactory.getPNRequestDao, null)
    ServiceFactory.initAuthorisationService(DaoFactory.getPrivDao, DaoFactory.getUserInfoDao)
    ServiceFactory.initStorageService(DaoFactory.getKeyChainDao)

    ConnektLogger(LogFile.SERVICE).info("BaseReceptorsTest bootstrapped.")
  }
}
