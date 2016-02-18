package com.flipkart.connekt.busybees

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import com.flipkart.connekt.busybees.flows.KafkaMessageProcessFlow
import com.flipkart.connekt.busybees.processors.PNProcessor
import com.flipkart.connekt.commons.connections.ConnectionProvider
import com.flipkart.connekt.commons.core.BaseApp
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ServiceFactory, ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.{ConnektConfig, DeviceDetailsService}
import com.flipkart.connekt.commons.utils.{ConfigUtils, StringUtils}
import com.typesafe.config.ConfigFactory

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
object BusyBeesBoot extends BaseApp {

  val initialized = new AtomicBoolean(false)
  var pnDispatchFlow: Option[KafkaMessageProcessFlow[ConnektRequest, PNProcessor]] = None
  lazy implicit val system = ActorSystem("busyBees-system")

  def start() {

    if (!initialized.get()) {
      ConnektLogger(LogFile.SERVICE).info("BusyBees initializing.")

      val configFile = ConfigUtils.getSystemProperty("logback.config").getOrElse("logback-busybees.xml")
      val logConfigFile = getClass.getClassLoader.getResourceAsStream(configFile)

      ConnektLogger(LogFile.SERVICE).info(s"BusyBees Logging using $configFile")
      ConnektLogger.init(logConfigFile)

      ConnektConfig(configServiceHost, configServicePort)()

      DaoFactory.setUpConnectionProvider(new ConnectionProvider)

      val hConfig = ConnektConfig.getConfig("busybees.connections.hbase")
      DaoFactory.initHTableDaoFactory(hConfig.get)

      val mysqlConf = ConnektConfig.getConfig("receptors.connections.mysql").getOrElse(ConfigFactory.empty())
      DaoFactory.initMysqlTableDaoFactory(mysqlConf)

      val couchbaseCf = ConnektConfig.getConfig("receptors.connections.couchbase").getOrElse(ConfigFactory.empty())
      DaoFactory.initCouchbaseCluster(couchbaseCf)

      ServiceFactory.initStorageService(DaoFactory.getStorageDao)

      val kafkaConnConf = ConnektConfig.getConfig("busybees.connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())
      val kafkaConsumerPoolConf = ConnektConfig.getConfig("busybees.connections.kafka.consumerPool").getOrElse(ConfigFactory.empty())
      ConnektLogger(LogFile.SERVICE).info(s"Kafka Conf: ${kafkaConnConf.toString}")
      val kafkaHelper = KafkaConsumerHelper(kafkaConnConf, kafkaConsumerPoolConf)

      println(DeviceDetailsService.get("ConnectSampleApp", StringUtils.generateRandomStr(15)))
      pnDispatchFlow = Some(new KafkaMessageProcessFlow[ConnektRequest, PNProcessor](kafkaHelper, "fk-connekt-pn", 1, 5)(system))
      pnDispatchFlow.foreach(_.run())

    }
  }

  def terminate() = {
    ConnektLogger(LogFile.SERVICE).info("BusyBees Shutting down.")
    if (initialized.get()) {
      DaoFactory.shutdownHTableDaoFactory()
      pnDispatchFlow.foreach(_.shutdown())
    }
  }

  def main(args: Array[String]) {
    System.setProperty("logback.config", "logback-test.xml")
    start()
  }
}
