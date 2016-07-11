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
package com.flipkart.connekt.commons.tests.services

import java.util.Properties

import com.flipkart.connekt.commons.helpers.KafkaConnectionHelper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.typesafe.config.ConfigFactory
import kafka.consumer.ConsumerConnector
import kafka.producer.{KeyedMessage, Producer}
import org.apache.commons.pool.impl.GenericObjectPool

class KafkaConnectionHelperTest extends CommonsBaseTest with KafkaConnectionHelper {

  val topicName = "fk-connekt-proto"
  var kafkaConsumerPool: GenericObjectPool[ConsumerConnector] = null
  var kafkaProducerPool: GenericObjectPool[Producer[String, String]] = null

  def createKafkaConsumerFactory = {
    val consumerConnProps = new Properties()
    consumerConnProps.setProperty("zookeeper.connect", ConnektConfig.getString("connections.kafka.consumerConnProps.zookeeper.connect").getOrElse("10.32.197.54:2181,10.32.165.183:2181,10.32.237.188:2181/kafka/preprod6")  )
    consumerConnProps.setProperty("group.id", "1")
    consumerConnProps.setProperty("zookeeper.session.timeout.ms", "5000")
    consumerConnProps.setProperty("zookeeper.sync.time.ms", "200")
    consumerConnProps.setProperty("auto.commit.interval.ms", "1000")
    
    val consumerFactoryConf = ConfigFactory.parseProperties(consumerConnProps)
    createKafkaConsumerPool(consumerFactoryConf, Some(5), Some(1), Some(1000L * 60L * 30L), Some(-1), enableLifo = false)
  }

  def createKafkaProducerFactory = {
    val producerConnProps = new Properties()
    producerConnProps.setProperty("metadata.broker.list",ConnektConfig.getString("connections.kafka.producerConnProps.metadata.broker.list").getOrElse( "10.33.217.147:9092"))
    producerConnProps.setProperty("serializer.class", "kafka.serializer.StringEncoder")
    producerConnProps.setProperty("request.required.acks", "0")

    val consumerFactoryConf = ConfigFactory.parseProperties(producerConnProps)
    createKafkaProducerPool(consumerFactoryConf, Some(5), Some(1), Some(1000L * 60L * 30L), Some(-1), enableLifo = false)
  }

  "Initialising producer factory" should "succeed" in {
    noException should be thrownBy {
      kafkaProducerPool = createKafkaProducerFactory
    }
  }

  "Initialising consumer factory" should "succeed" in {
    noException should be thrownBy {
      kafkaConsumerPool = createKafkaConsumerFactory
    }
  }

  "Sending a keyed-message" should "succeed" in {
    val producer = kafkaProducerPool.borrowObject()
    try {
        noException should be thrownBy producer.send(new KeyedMessage[String, String](topicName, "SampleProtoMessage at %s".format(System.currentTimeMillis)))
    } finally {
      kafkaProducerPool.returnObject(producer)
    }
  }

//  "Consuming messages" should "succeed" in {
//    val consumer = kafkaConsumerPool.borrowObject()
//    try {
//      val streams = consumer.createMessageStreams(Map[String, Int](topicName -> 1))
//      streams.keys.foreach(topic => {
//          streams.get(topic).map(_.zipWithIndex).foreach(l => {
//            println("Reading streams for topic %s".format(topic))
//            l.foreach(x => {
//              val streamIterator = x._1.iterator()
//              while (streamIterator.hasNext()) {
//                val msg = streamIterator.next()
//                println("stream: %s message: %s".format(x._2, new String(msg.message)))
//              }
//            })
//          })
//      })
//    } finally {
//      kafkaConsumerPool.returnObject(consumer)
//    }
//  }

  override def afterAll() = {
    println("triggering cleanup afterAll")
    Option(kafkaConsumerPool).foreach(_.close())
    Option(kafkaProducerPool).foreach(_.close())
    println("cleanup successful")
  }
}
