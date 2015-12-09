package com.flipkart.connekt.receptors

import java.util.concurrent.atomic.AtomicBoolean

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ServiceFactory, LogFile, ConnektLogger}
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
object ReceptorsBoot {

  val initialized = new AtomicBoolean(false)
  var receptors: ReceptorsServer = null
  var httpBindFuture = null

  def start() {
    if (!initialized.get()) {
      ConnektLogger(LogFile.SERVICE).info("ReceptorsBoot initializing.")

      ConnektConfig(configHost = "config-service.nm.flipkart.com", configPort = 80, configAppVersion = 1)

      val logConfigFile =  getClass.getClassLoader.getResource("logback.xml").getFile
      ConnektLogger(LogFile.SERVICE).info(s"ReceptorsBoot Log Config $logConfigFile")
      ConnektLogger.init(logConfigFile)

      val hConfig = ConnektConfig.getConfig("receptors.connections.hbase")
      DaoFactory.initHTableDaoFactory(hConfig.get)

      val kafkaConnConf = ConnektConfig.getConfig("receptors.connections.kafka.producerConnProps").getOrElse(ConfigFactory.empty())
      val kafkaProducerPoolConf = ConnektConfig.getConfig("receptors.connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
      KafkaProducerHelper.init(kafkaConnConf, kafkaProducerPoolConf)

      ServiceFactory.initMessageService(DaoFactory.getRequestInfoDao, KafkaProducerHelper, null)
      ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao)

      receptors = new ReceptorsServer
      receptors.init
      ConnektLogger(LogFile.SERVICE).info("Receptors initialized.")
    }
  }

  def terminate() = {
    ConnektLogger(LogFile.DAO).info("Receptors terminating.")
    if(initialized.get()) {
      DaoFactory.shutdownHTableDaoFactory()
      receptors.stop()
    }
  }

  def main(args: Array[String]) {
    val logConfigFile =  getClass.getClassLoader.getResource("logback.xml").getFile
    println(logConfigFile)
  }
}
