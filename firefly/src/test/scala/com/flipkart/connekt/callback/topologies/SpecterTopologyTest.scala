package com.flipkart.connekt.callback.topologies

import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.iomodels.{CallbackEvent, PNCallbackEvent}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.{ConnektConfig, SubscriptionService}
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.firefly.FireflyBoot
import com.typesafe.config.ConfigFactory
import jdk.nashorn.internal.ir.annotations.Ignore

@Ignore
class SpecterTopologyTest extends TopologyUTSpec with Instrumented {
  val id = "8f3e95bb-d957-4a6e-a694-fc948b120439"
  "HttpTopology Test" should "run" in {

    FireflyBoot.start()

    val subscription = SubscriptionService.get(id).get.get

    SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION, List("start", subscription.getJson)))

    val kafkaProducerConnConf = ConnektConfig.getConfig("connections.kafka.producerConnProps").get
    val kafkaProducerPoolConf = ConnektConfig.getConfig("connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    val kafkaProducerHelper = KafkaProducerHelper.init(kafkaProducerConnConf, kafkaProducerPoolConf)

    val pncallback = PNCallbackEvent(messageId = StringUtils.generateRandomStr(8),
                                     clientId = StringUtils.generateRandomStr(8),
                                     deviceId = StringUtils.generateRandomStr(8),
                                     eventType = StringUtils.generateRandomStr(8),
                                     platform = StringUtils.generateRandomStr(8),
                                     appName = StringUtils.generateRandomStr(8),
                                     contextId = StringUtils.generateRandomStr(8))
//    println(pncallback.getJson)
    val x = pncallback.getJson.getObj[CallbackEvent].getJson
//    println("X = " + x)
    val time1 = System.currentTimeMillis()
    for( a <- 1 to 1000) {
      kafkaProducerHelper.writeMessages("ckt_callback_events", x)
    }

    val time2 =  System.currentTimeMillis()

    println(" time taken = " + (time2 - time1))


    Thread.sleep(100000)

    SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION, List("stop", subscription.getJson)))

    Thread.sleep(1000)

  }

}
