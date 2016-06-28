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
package com.flipkart.connekt.callback.topologies

import java.util.UUID

import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.firefly.FireflyBoot
import com.flipkart.connekt.commons.entities.{Transformers, GenericAction, HTTPEventSink, Subscription}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.{ConnektConfig, SubscriptionService}
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.ConfigFactory

class HttpTopologyTest extends TopologyUTSpec {

  "HttpTopology Test" should "run" in {

    FireflyBoot.start()

    val subscriptionSuccess = new Subscription()

    subscriptionSuccess.name = "HttpTopologyTest"
    subscriptionSuccess.id = "e7fe4e8b-ea16-402a-b0b6-7568eadc0bf6"
    subscriptionSuccess.shutdownThreshold = 3
    subscriptionSuccess.createdBy = "connekt-genesis"
    subscriptionSuccess.sink = new HTTPEventSink("POST", "http://requestb.in/wis41kwi")
    subscriptionSuccess.eventFilter = "testEventFilter"
    subscriptionSuccess.eventTransformer = new Transformers("testHeader", "testPayload")


    SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION, List(GenericAction.START.toString, subscriptionSuccess.getJson)))

    val kafkaProducerConnConf = ConnektConfig.getConfig("connections.kafka.producerConnProps").get
    val kafkaProducerPoolConf = ConnektConfig.getConfig("connections.kafka.producerPool").getOrElse(ConfigFactory.empty())
    val kafkaProducerHelper = KafkaProducerHelper.init(kafkaProducerConnConf, kafkaProducerPoolConf)



    val msg =s"""
             {"type":"PN","messageId":"9c75d26e-f82a-42e9-8896-41fcbab27369","clientId":"connekt-insomnia","deviceId":"6aebb89b060c442764f7b940a16e109c","eventType":"gcm_received","platform":"android","appName":"retailapp","contextId":"","cargo":"0:1466498004107001%c7d00653f9fd7ecd","timestamp":1466498004226}
      """

    kafkaProducerHelper.writeMessages("active_events", msg)

    Thread.sleep(1000000)

  }

}
