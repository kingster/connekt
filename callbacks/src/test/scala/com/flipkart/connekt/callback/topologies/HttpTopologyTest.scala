package com.flipkart.connekt.callback.topologies

import java.util.UUID

import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.callbacks.ClientTopologyManager
import com.flipkart.connekt.commons.entities.{HTTPRelayPoint, Subscription, SubscriptionAction}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await

/**
  * Created by harshit.sinha on 21/06/16.
  */
class HttpTopologyTest extends TopologyUTSpec {

  "HttpTopology Test" should "run" in {

    ClientTopologyManager()
    val subscription = new Subscription()
    subscription.name = "endTest"
    subscription.id = "35bfea58-7166-44a1-8985-e8e8b96249a7"
    subscription.shutdownThreshold = 3
    subscription.createdBy = "connekt-insomnia"
    subscription.relayPoint = new HTTPRelayPoint("http://localhost:8080/receive")
    subscription.groovyFilter = """
                                  |package com.flipkart.connekt.commons.entities;
                                  |import com.flipkart.connekt.commons.iomodels.CallbackEvent
                                  |import com.flipkart.connekt.commons.iomodels.PNCallbackEvent;
                                  |class ConnektSampleAppGroovy implements Evaluator {
                                  |public boolean evaluate(CallbackEvent context) {
                                  |return (context as PNCallbackEvent).eventType().equals("gcm_received")
                                  |}
                                  |
                                  |}
                                """.stripMargin
    SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION, List(SubscriptionAction.START, subscription)))

    val kafkaProducerConnConf = ConnektConfig.getConfig("connections.kafka.producerConnProps").get
    val kafkaProducerPoolConf = ConnektConfig.getConfig("connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    val kafkaProducerHelper = KafkaProducerHelper.init(kafkaProducerConnConf, kafkaProducerPoolConf)
    val msg =s"""
             {"type":"PN","messageId":"9c75d26e-f82a-42e9-8896-41fcbab27369","clientId":"connekt-insomnia","deviceId":"6aebb89b060c442764f7b940a16e109c","eventType":"gcm_received","platform":"android","appName":"retailapp","contextId":"","cargo":"0:1466498004107001%c7d00653f9fd7ecd","timestamp":1466498004226}
      """
    kafkaProducerHelper.writeMessages("active_events", msg)

    Thread.sleep(10000)
  }

}
