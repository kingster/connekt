package com.flipkart.connekt.receptors

import java.util.concurrent.atomic.AtomicBoolean

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
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
object ReceptorsBoot extends App {
  val initialized = new AtomicBoolean(false)
  var receptors: ReceptorsServer = null
  var httpBindFuture = null

  if(!initialized.get()) {
    ConnektConfig(configHost = "config-service.nm.flipkart.com", configPort = 80, configAppVersion = 1)
    val logConfigFile = System.getProperty("user.dir").concat("/receptors/src/main/resources/logback.xml")
    ConnektLogger.init(logConfigFile)
    ConnektLogger(LogFile.SERVICE).info("ReceptorsBoot initializing.")

    val hConfig = ConnektConfig.getConfig("receptors.connections.hbase")
    DaoFactory.initHTableDaoFactory(hConfig.get)

    val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)

    receptors = new ReceptorsServer
    receptors.init
    ConnektLogger(LogFile.SERVICE).info("Receptors initialized.")
  }

  def terminate() = {
    ConnektLogger(LogFile.DAO).info("Receptors terminating.")
    DaoFactory.shutdownHTableDaoFactory()
    receptors.stop()
  }
}
