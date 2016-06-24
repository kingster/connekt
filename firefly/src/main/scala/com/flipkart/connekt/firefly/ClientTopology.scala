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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.flipkart.connekt.busybees.streams.sources.CallbackKafkaSource
import com.flipkart.connekt.firefly.sinks.http.HttpSink
import com.flipkart.connekt.firefly.sinks.kafka.KafkaSink
import com.flipkart.connekt.commons.entities._
import com.flipkart.connekt.commons.entities.fabric.FabricMaker
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.typesafe.config.Config

import scala.concurrent.Promise

class ClientTopology(topic: String, retryLimit: Int, kafkaConsumerConnConf: Config, subscription: Subscription)(implicit am: ActorMaterializer, sys: ActorSystem) {

  var kafkaCallbackSource: CallbackKafkaSource[CallbackEvent] = _
  implicit val ec = am.executionContext

  def start(): Promise[String] = {
    val topologyShutdownTrigger = Promise[String]()
    val evaluator = FabricMaker.create[Evaluator](subscription.id, subscription.eventFilter)
    kafkaCallbackSource = new CallbackKafkaSource[CallbackEvent](topic, subscription.id, kafkaConsumerConnConf)(topologyShutdownTrigger.future)
    val source = Source.fromGraph(kafkaCallbackSource).filter(evaluator.evaluate)
    subscription.sink match {
      case http: HTTPEventSink => source.runWith(new HttpSink(subscription, retryLimit, topologyShutdownTrigger).getHttpSink)
      case kafka: KafkaEventSink => source.runWith(new KafkaSink(kafka.broker, kafka.zookeeper).getKafkaSink)
    }
    topologyShutdownTrigger
  }
}
