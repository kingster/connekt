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
package com.flipkart.connekt.busybees

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.stream.{Supervision, ActorMaterializer, ActorMaterializerSettings}
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpDispatcher
import com.flipkart.connekt.busybees.streams.topologies.PushTopology
import com.flipkart.connekt.commons.connections.ConnectionProvider
import com.flipkart.connekt.commons.core.BaseApp
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.services.{BigfootService, ConnektConfig, DeviceDetailsService}
import com.flipkart.connekt.commons.sync.SyncManager
import com.flipkart.connekt.commons.utils.{ConfigUtils, StringUtils}
import com.typesafe.config.ConfigFactory

object BusyBeesBoot extends BaseApp {

  private val initialized = new AtomicBoolean(false)

  implicit val system = ActorSystem("busyBees-system")

  val settings = ActorMaterializerSettings(system)
    .withDispatcher("akka.actor.default-dispatcher")
    .withAutoFusing(enable = false)

  lazy implicit val mat = ActorMaterializer(settings)

  implicit val stageSupervisionDecider: Supervision.Decider = {
    case cEx: ConnektPNStageException =>
      val callbackEvents = cEx.deviceId.map(PNCallbackEvent(cEx.messageId, _, cEx.eventType, cEx.platform, cEx.appName, cEx.context, cEx.getMessage, cEx.timeStamp))
      callbackEvents.foreach(e => ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, s"${e.appName}${e.deviceId}", Channel.PUSH, e))
      callbackEvents.foreach(e => BigfootService.ingest(e.toBigfootFormat))
      Supervision.Resume

    case _ => Supervision.Stop
  }

  var pushTopology: PushTopology = _

  def start() {

    if (!initialized.getAndSet(true)) {
      ConnektLogger(LogFile.SERVICE).info("BusyBees initializing.")

      val configFile = ConfigUtils.getSystemProperty("log4j.configurationFile").getOrElse("log4j2-busybees.xml")

      ConnektLogger(LogFile.SERVICE).info(s"BusyBees Logging using $configFile")
      ConnektLogger.init(configFile)

      ConnektConfig(configServiceHost, configServicePort)(Seq("fk-connekt-root", "fk-connekt-".concat(ConfigUtils.getConfEnvironment),"fk-connekt-busybees", "fk-connekt-busybees-akka"))

      SyncManager.create(ConnektConfig.getString("sync.zookeeper").get)

      DaoFactory.setUpConnectionProvider(new ConnectionProvider)

      val hConfig = ConnektConfig.getConfig("connections.hbase")
      DaoFactory.initHTableDaoFactory(hConfig.get)

      val mysqlConf = ConnektConfig.getConfig("connections.mysql").getOrElse(ConfigFactory.empty())
      DaoFactory.initMysqlTableDaoFactory(mysqlConf)

      val couchbaseCf = ConnektConfig.getConfig("connections.couchbase").getOrElse(ConfigFactory.empty())
      DaoFactory.initCouchbaseCluster(couchbaseCf)

      ServiceFactory.initStorageService(DaoFactory.getKeyChainDao)
      ServiceFactory.initCallbackService(null, DaoFactory.getPNCallbackDao, DaoFactory.getPNRequestDao, null)

      val kafkaConnConf = ConnektConfig.getConfig("connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())
      val kafkaConsumerPoolConf = ConnektConfig.getConfig("connections.kafka.consumerPool").getOrElse(ConfigFactory.empty())
      ConnektLogger(LogFile.SERVICE).info(s"Kafka Conf: ${kafkaConnConf.toString}")
      val kafkaHelper = KafkaConsumerHelper(kafkaConnConf, kafkaConsumerPoolConf)

      ServiceFactory.initPNMessageService(DaoFactory.getPNRequestDao, DaoFactory.getUserConfigurationDao, null, kafkaHelper)

      //TODO : Fix this, this is for bootstraping hbase connection.
      println(DeviceDetailsService.get("ConnectSampleApp",  StringUtils.generateRandomStr(15)))

      HttpDispatcher.init(ConnektConfig.getConfig("react").get)

      pushTopology = new PushTopology(kafkaHelper)
      pushTopology.run
    }
  }

  def terminate() = {
    ConnektLogger(LogFile.SERVICE).info("BusyBees Shutting down.")
    if (initialized.get()) {
      DaoFactory.shutdownHTableDaoFactory()
      Option(pushTopology).foreach(_.shutdown())
    }
  }

  def main(args: Array[String]) {
    System.setProperty("log4j.configurationFile", "log4j2-test.xml")
    start()
  }
}
