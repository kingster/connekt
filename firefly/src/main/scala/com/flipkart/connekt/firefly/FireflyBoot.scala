/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.firefly

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.flipkart.connekt.busybees.streams.flows.StageSupervision
import com.flipkart.connekt.commons.connections.ConnectionProvider
import com.flipkart.connekt.commons.core.BaseApp
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncManager
import com.flipkart.connekt.commons.utils.ConfigUtils
import com.typesafe.config.ConfigFactory

object FireflyBoot extends BaseApp {

  private val initialized = new AtomicBoolean(false)

  private implicit val system = ActorSystem("callback-system")

  val settings = ActorMaterializerSettings(system)
    .withAutoFusing(enable = false)
    .withSupervisionStrategy(StageSupervision.decider)

  private implicit val mat = ActorMaterializer(settings.withDispatcher("akka.actor.default-dispatcher"))
  private implicit val ec = mat.executionContext

  def start() {
    if (!initialized.getAndSet(true)) {
      ConnektLogger(LogFile.SERVICE).info("Callback service initializing.")

      val configFile = ConfigUtils.getSystemProperty("log4j.configurationFile").getOrElse("log4j2-firefly.xml")

      ConnektLogger(LogFile.SERVICE).info(s"Callback service logging using: $configFile")
      ConnektLogger.init(configFile)

      ConnektConfig(configServiceHost, configServicePort)(Seq("fk-connekt-root", "fk-connekt-".concat(ConfigUtils.getConfEnvironment) , "fk-connekt-firefly"))

      SyncManager.create(ConnektConfig.getString("sync.zookeeper").get)

      DaoFactory.setUpConnectionProvider(new ConnectionProvider)

      val mysqlConf = ConnektConfig.getConfig("connections.mysql").getOrElse(ConfigFactory.empty())
      DaoFactory.initMysqlTableDaoFactory(mysqlConf)

      ServiceFactory.initStencilService(DaoFactory.getStencilDao)

      val kafkaConnConf = ConnektConfig.getConfig("connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())

      ClientTopologyManager(kafkaConnConf, ConnektConfig.getString("firefly.kafka.topic").get, ConnektConfig.getInt("callbacks.retry.limit").get)

      ConnektLogger(LogFile.SERVICE).info("Started `Firefly` app")
    }
  }

  def terminate() = {
    ConnektLogger(LogFile.SERVICE).info("BusyBees shutting down")
    if (initialized.get()) {
      DaoFactory.shutdownHTableDaoFactory()
      Option(ClientTopologyManager.instance).foreach(_.stopAllTopologies())
      ConnektLogger.shutdown()
    }
  }

  def main(args: Array[String]) {
    start()
  }
}
