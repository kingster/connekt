package com.flipkart.connekt.busybees.flows

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{Actor, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.utils.StringUtils._
import kafka.consumer.KafkaStream
import kafka.serializer.{Decoder, DefaultDecoder}

import scala.reflect.ClassTag


/**
 *
 *
 * @author durga.s
 * @version 12/3/15
 */
trait ProcessFlow {
  def run()
  def shutdown()
}

class MessageDecoder[T: ClassTag](implicit tag: ClassTag[T]) extends Decoder[T] {
  override def fromBytes(bytes: Array[Byte]): T = objMapper.readValue(bytes.getString, tag.runtimeClass).asInstanceOf[T]
}

/**
 *
 * KafkaMessageProcessFlow models a generic processing pipe for a specific kafka topic
 * messages, using any custom actor class.
 *
 * @param kafkaHelper kafka broker connection pool helper
 * @param topic kafka topic to establish stream with
 * @param numStreams #numStreams of topic
 * @param numWorkers #numWorkers for immediate message relay
 * @param context implicit actorSystem context
 * @tparam M De-serialized kafka message type
 * @tparam A Class Type of message receiving actors
 */
class KafkaMessageProcessFlow[M: ClassTag, A <: akka.actor.Actor: ClassTag](kafkaHelper: KafkaConsumerHelper, topic: String, numStreams: Int, numWorkers: Int)(context: ActorSystem) extends ProcessFlow {

  val isLive = new AtomicBoolean(false)
  val noStreams = List.empty[KafkaStream[Array[Byte], M]]
  val kafkaConnector = kafkaHelper.getConnector

  val streams =  kafkaConnector.createMessageStreams[Array[Byte], M](
    Map[String, Int](topic -> numStreams),
    new DefaultDecoder(),
    new MessageDecoder[M]()
  ).getOrElse[List[KafkaStream[Array[Byte], M]]](topic, noStreams)

  lazy val supervisor = context.actorOf(Props(classOf[Supervisor], this), "kafka-flow-supervisor")

  def run() = {
    isLive.set(true)
    streams.foreach(s => {
      try {
        val i = s.iterator()
        while(isLive.get && i.hasNext())
          supervisor ! i.next().message()
      } catch {
        case e: Exception =>
          kafkaConnector.commitOffsets
          ConnektLogger(LogFile.SERVICE).error("KafkaMessageProcessFlow error: ${e.getMessage}", e)
      }
    })
  }

  def shutdown() = {
    isLive.set(false)
    kafkaConnector.shutdown()
  }

  private class Supervisor extends Actor {
    var router = {
      val processWorkers = Vector.fill(numWorkers) {
        val r = context.actorOf(Props[A])
        context watch r
        ActorRefRoutee(r)
      }
      Router(RoundRobinRoutingLogic(), processWorkers)
    }

    override def receive: Receive = {
      case message: M =>
        router.route(message, sender())
      case Terminated(a) =>
        router = router.removeRoutee(a)
        val r = context.actorOf(Props[A])
        context watch r
        router = router.addRoutee(r)
    }
  }
}