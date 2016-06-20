package callback

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.flipkart.connekt.busybees.streams.flows.StageSupervision
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpDispatcher
import com.flipkart.connekt.commons.connections.ConnectionProvider
import com.flipkart.connekt.commons.core.BaseApp
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.{KafkaConsumerHelper, KafkaProducerHelper}
import com.flipkart.connekt.commons.services.{ConnektConfig, DeviceDetailsService}
import com.flipkart.connekt.commons.sync.SyncManager
import com.flipkart.connekt.commons.utils.{ConfigUtils, StringUtils}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

/**
  * Created by harshit.sinha on 13/06/16.
  */
object CallbackBoot extends BaseApp {


  private val initialized = new AtomicBoolean(false)

  implicit val system = ActorSystem("callback-system")

  val settings = ActorMaterializerSettings(system)
    .withAutoFusing(enable = false) //TODO: Enable async boundaries and then enable auto-fusing
    .withSupervisionStrategy(StageSupervision.decider)

  lazy implicit val mat = ActorMaterializer(settings.withDispatcher("akka.actor.default-dispatcher"))
  implicit val ec = mat.executionContext
  val dispatcher = system.dispatcher

  lazy val ioMat = ActorMaterializer(settings.withDispatcher("akka.actor.io-dispatcher"))

  def start() {

    if (!initialized.getAndSet(true)) {
      ConnektLogger(LogFile.SERVICE).info("Callback service initializing.")

      val configFile = ConfigUtils.getSystemProperty("log4j.configurationFile").getOrElse("log4j2-busybees.xml")

      ConnektLogger(LogFile.SERVICE).info(s"Callback logging using: $configFile")
      ConnektLogger.init(configFile)

      ConnektConfig(configServiceHost, configServicePort)(Seq("fk-connekt-root", "fk-connekt-".concat(ConfigUtils.getConfEnvironment), "fk-connekt-busybees", "fk-connekt-busybees-akka"))

      SyncManager.create(ConnektConfig.getString("sync.zookeeper").get)

      DaoFactory.setUpConnectionProvider(new ConnectionProvider)

      val mysqlConf = ConnektConfig.getConfig("connections.mysql").getOrElse(ConfigFactory.empty())
      DaoFactory.initMysqlTableDaoFactory(mysqlConf)

      val kafkaConnConf = ConnektConfig.getConfig("connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())
      val kafkaConsumerPoolConf = ConnektConfig.getConfig("connections.kafka.consumerPool").getOrElse(ConfigFactory.empty())
      ConnektLogger(LogFile.SERVICE).info(s"Kafka Conf: ${kafkaConnConf.toString}")
      val kafkaHelper = KafkaConsumerHelper(kafkaConnConf, kafkaConsumerPoolConf)


      HttpDispatcher.init(ConnektConfig.getConfig("react").get)

      ClientTopologyManager()
    }
  }

  def main(args: Array[String]) {
    start()
  }
}
