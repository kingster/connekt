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
import com.flipkart.connekt.callbacks.sinks.http.{HttpCallbackTracker, HttpSink}
import com.flipkart.connekt.callbacks.sinks.kafka.KafkaSink
import com.flipkart.connekt.commons.entities._
import com.flipkart.connekt.commons.entities.fabric.FabricMaker
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Promise}

class ClientTopology(subscription: Subscription)(implicit topic: String,retryLimit: Int, kafkaConsumerConnConf: Config, am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) {

  val evaluator = FabricMaker.create[Evaluator](subscription.id, subscription.groovyFilter)
  val topologyShutdownTrigger = Promise[String]()
  var kafkaCallbackSource: CallbackKafkaSource[CallbackEvent] = _

  private def httpPrepare(event: CallbackEvent): (HttpRequest, HttpCallbackTracker) = {
    val url = new URL(subscription.relayPoint.asInstanceOf[HTTPRelayPoint].url)
    val payload = event.getJson
    val httpEntity = HttpEntity(ContentTypes.`application/json`, payload)
    val httpRequest = HttpRequest(method = HttpMethods.POST, uri = url.getPath, entity = httpEntity)
    val callbackTracker = new HttpCallbackTracker(payload, 0, false)
    (httpRequest, callbackTracker)
  }

  def start(): Promise[String] = {
    kafkaCallbackSource = new CallbackKafkaSource[CallbackEvent](subscription.id)(topologyShutdownTrigger.future)
    val source = Source.fromGraph(kafkaCallbackSource).filter(evaluator.evaluate)
    subscription.relayPoint match {
      case http: HTTPRelayPoint => source.map(httpPrepare).runWith(new HttpSink(subscription, topologyShutdownTrigger).getHttpSink())
      case kafka: KafkaRelayPoint => source.runWith(new KafkaSink(kafka.broker, kafka.zookeeper).getKafkaSink)
    }

    topologyShutdownTrigger
  }

}
