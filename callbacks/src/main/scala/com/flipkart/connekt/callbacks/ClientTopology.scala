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
package com.flipkart.connekt.callbacks

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.flipkart.connekt.busybees.streams.sources.CallbackKafkaSource
import com.flipkart.connekt.callbacks.sinks.http.{HttpCallbackTracker, HttpSink}
import com.flipkart.connekt.callbacks.sinks.kafka.KafkaSink
import com.flipkart.connekt.commons.entities._
import com.flipkart.connekt.commons.entities.fabric.FabricMaker
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.Config

import scala.concurrent.Promise

class ClientTopology(topic: String, retryLimit: Int, kafkaConsumerConnConf: Config, subscription: Subscription)(implicit am: ActorMaterializer, sys: ActorSystem) {

  var kafkaCallbackSource: CallbackKafkaSource[CallbackEvent] = _
  implicit val ec = am.executionContext

  def start(): Promise[String] = {
    val topologyShutdownTrigger = Promise[String]()
    val evaluator = FabricMaker.create[Evaluator](subscription.id, subscription.groovyFilter)
    kafkaCallbackSource = new CallbackKafkaSource[CallbackEvent](topic, subscription.id, kafkaConsumerConnConf)(topologyShutdownTrigger.future)
    val source = Source.fromGraph(kafkaCallbackSource).filter(evaluator.evaluate)
    subscription.relayPoint match {
      case http: HTTPRelayPoint => source.map(httpPrepare).runWith(new HttpSink(subscription, retryLimit, topologyShutdownTrigger).getHttpSink())
      case kafka: KafkaRelayPoint => source.runWith(new KafkaSink(kafka.broker, kafka.zookeeper).getKafkaSink)
    }
    topologyShutdownTrigger
  }

  private def httpPrepare(event: CallbackEvent): (HttpRequest, HttpCallbackTracker) = {
    val payload = event.getJson
    val httpEntity = HttpEntity(ContentTypes.`application/json`, payload)
    val httpRequest = HttpRequest(method = HttpMethods.POST, uri = subscription.relayPoint.asInstanceOf[HTTPRelayPoint].url, entity = httpEntity)
    val callbackTracker = HttpCallbackTracker(payload)
    (httpRequest, callbackTracker)
  }
}
