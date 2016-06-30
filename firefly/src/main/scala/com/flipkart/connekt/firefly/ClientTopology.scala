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
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.commons.entities._
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.firefly.sinks.http.HttpSink
import com.flipkart.connekt.firefly.sinks.kafka.KafkaSink
import com.typesafe.config.Config

import scala.collection.JavaConverters._
import scala.concurrent.Promise
import com.roundeights.hasher.Implicits._

class ClientTopology(topic: String, retryLimit: Int, kafkaConsumerConnConf: Config, subscription: Subscription)(implicit am: ActorMaterializer, sys: ActorSystem) {

  var kafkaCallbackSource: KafkaSource[CallbackEvent] = _
  implicit val ec = am.executionContext

  def evaluator(data: CallbackEvent): Boolean = {
    Option(subscription.eventFilter).forall { stencilId =>
      ServiceFactory.getStencilService.get(stencilId).find(_.component == "eventFilter") match {
        case Some(stencil) => ServiceFactory.getStencilService.materialize(stencil, data.getJson.getObj[ObjectNode]).asInstanceOf[Boolean]
        case None => true
      }
    }
  }

  def start(): Promise[String] = {

    val topologyShutdownTrigger = Promise[String]()
    kafkaCallbackSource = new KafkaSource[CallbackEvent](kafkaConsumerConnConf, topic, subscription.id.crc32.hash.hex)(topologyShutdownTrigger.future)
    val source = Source.fromGraph(kafkaCallbackSource).filter(evaluator).map(transform).filter(null != _.payload)
    subscription.sink match {
      case http: HTTPEventSink => source.runWith(new HttpSink(subscription, retryLimit, topologyShutdownTrigger).getHttpSink)
      case kafka: KafkaEventSink => source.runWith(new KafkaSink(kafka.topic, kafka.broker).getKafkaSink)
    }

    topologyShutdownTrigger
  }

  def transform(event: CallbackEvent): SubscriptionEvent = {

    Option(subscription.eventTransformer).map { transformer =>
      val stencilService = ServiceFactory.getStencilService
      SubscriptionEvent(header = stencilService.get(transformer.header).find(_.component == "header") match {
        case Some(stencil) => stencilService.materialize(stencil, event.getJson.getObj[ObjectNode]).asInstanceOf[java.util.HashMap[String, String]].asScala.toMap
        case None => null
      }, payload = stencilService.get(transformer.payload).find(_.component == "payload") match {
        case Some(stencil) => stencilService.materialize(stencil, event.getJson.getObj[ObjectNode])
        case None => event
      })
    }.getOrElse(SubscriptionEvent(null, event))

  }
}
