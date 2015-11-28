package com.flipkart.connekt.busybees

import java.util.concurrent.atomic.AtomicBoolean

import com.flipkart.connekt.busybees.controllers.DummyWorker
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.typesafe.config.ConfigFactory

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
object BootBusyBees extends App {
  val initialized = new AtomicBoolean(false)

  if(!initialized.get()) {
    ConnektConfig(configHost = "config-service.nm.flipkart.com", configPort = 80, configAppVersion = 1)
    val logConfigFile = System.getProperty("user.dir").concat("/busybees/src/main/resources/logback.xml")
    ConnektLogger.init(logConfigFile)
    ConnektLogger(LogFile.SERVICE).info("BusyBees initializing.")

    val hConfig = ConnektConfig.getConfig("busybees.connections.hbase")
    DaoFactory.initHTableDaoFactory(hConfig.get)

    val kafkaConnConf = ConnektConfig.getConfig("busybees.connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())
    val kafkaConsumerPoolConf = ConnektConfig.getConfig("busybees.connections.kafka.consumerPool").getOrElse(ConfigFactory.empty())

    ConnektLogger(LogFile.SERVICE).info("Kafka Conf: %s".format(kafkaConnConf.toString))
    KafkaConsumerHelper.init(kafkaConnConf, kafkaConsumerPoolConf)

    DummyWorker.init()
  }

  def terminate = {
    DaoFactory.shutdownHTableDaoFactory()
  }
}
