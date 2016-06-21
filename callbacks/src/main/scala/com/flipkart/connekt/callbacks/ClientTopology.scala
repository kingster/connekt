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

import java.net.URL
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.flipkart.connekt.busybees.streams.sources.CallbackKafkaSource
import com.flipkart.connekt.callbacks.sinks.HttpSink.{HttpCallbackTracker, HttpSink}
import com.flipkart.connekt.callbacks.sinks.KafkaSink.KafkaSink
import com.flipkart.connekt.commons.entities._
import com.flipkart.connekt.commons.entities.fabric.FabricMaker
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.services.ConnektConfig
import com.typesafe.config.ConfigFactory
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.{ExecutionContext, Promise}

class ClientTopology(subscription: Subscription)(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) {

  val myEvaluator = FabricMaker.create[Evaluator](subscription.id, subscription.groovyFilter)
  val topologyShutdownTrigger = Promise[String]()
  var kafkaCallbackSource: CallbackKafkaSource[CallbackEvent] = _
  val kafkaConsumerConnConf = ConnektConfig.getConfig("connections.kafka.consumerConnProps").getOrElse(ConfigFactory.empty())
  val retryLimit = 4

  private def HttpPrepare(event: CallbackEvent): (HttpRequest, HttpCallbackTracker) = {
    val url = new URL(subscription.relayPoint.asInstanceOf[HTTPRelayPoint].url)
    val payload = event.getJson
    val httpEntity = HttpEntity(ContentTypes.`application/json`, payload)
    val httpRequest = HttpRequest(method = HttpMethods.POST, uri = url.getPath, entity = httpEntity)
    val callbackTracker = new HttpCallbackTracker(payload, 0, false)
    (httpRequest, callbackTracker)
  }

  def start(): Promise[String] = {
    kafkaCallbackSource = new CallbackKafkaSource[CallbackEvent](subscription.id, "active_events", kafkaConsumerConnConf)(topologyShutdownTrigger.future)
    val source = Source.fromGraph(kafkaCallbackSource).filter(myEvaluator.evaluate)
    subscription.relayPoint match {
      case http: HTTPRelayPoint => source.map(HttpPrepare).runWith(new HttpSink(subscription, topologyShutdownTrigger, retryLimit).getHttpSink())
      case kafka: KafkaRelayPoint => source.runWith(new KafkaSink(kafka.broker, kafka.zookeeper).getKafkaSink)
    }

    topologyShutdownTrigger
  }

}
