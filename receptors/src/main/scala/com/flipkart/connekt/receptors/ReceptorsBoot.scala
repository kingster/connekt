package com.flipkart.connekt.receptors

import java.util.concurrent.atomic.AtomicBoolean

import com.flipkart.connekt.commons.connections.ConnectionProvider
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.receptors.service.ReceptorsServer
import com.typesafe.config.ConfigFactory

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
object ReceptorsBoot  {

  private val initialized = new AtomicBoolean(false)

  def start() {
    if (!initialized.get()) {
      ConnektConfig(configHost = "config-service.nm.flipkart.com", configPort = 80, configAppVersion = 1)()

      val logConfigFile = getClass.getClassLoader.getResourceAsStream("logback-receptors.xml")
      ConnektLogger.init(logConfigFile)
      ConnektLogger(LogFile.SERVICE).info("Receptors initializing.")

      DaoFactory.setUpConnectionProvider(new ConnectionProvider())

      val hConfig = ConnektConfig.getConfig("receptors.connections.hbase")
      DaoFactory.initHTableDaoFactory(hConfig.get)

      val mysqlConf = ConnektConfig.getConfig("receptors.connections.mysql").getOrElse(ConfigFactory.empty())
      DaoFactory.initMysqlTableDaoFactory(mysqlConf)

      val couchbaseCf = ConnektConfig.getConfig("receptors.connections.couchbase").getOrElse(ConfigFactory.empty())
      DaoFactory.initCouchbaseCluster(couchbaseCf)

      val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
      val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
      KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)

      ServiceFactory.initMessageService(DaoFactory.getRequestInfoDao, KafkaProducerHelper, null)
      ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao, DaoFactory.getRequestInfoDao, null)
      ServiceFactory.initAuthorisationService(DaoFactory.getPrivDao, DaoFactory.getUserInfoDao)

      //Start up the receptors
      ReceptorsServer()

      ConnektLogger(LogFile.SERVICE).info("Receptors initialized.")
    }
  }

  def terminate() = {
    ConnektLogger(LogFile.DAO).info("Receptors terminating.")
    if (initialized.get()) {
      ReceptorsServer.shutdown()
      DaoFactory.shutdownHTableDaoFactory()
    }
  }

  def   main (args: Array[String]) {
    start()
  }
}
