package com.flipkart.connekt.commons.helpers

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.typesafe.config.Config
import kafka.producer.{KeyedMessage, Producer}
import org.apache.commons.pool.impl.GenericObjectPool

import scala.util.Try
import scala.util.control.NonFatal

/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
class KafkaProducerHelper(producerFactoryConf: Config, globalContextConf: Config) extends KafkaConnectionHelper with GenericObjectPoolHelper {

  validatePoolProps("kafka producer pool", globalContextConf)

  val kafkaProducerPool: GenericObjectPool[Producer[String, String]] = {
    try {
      createKafkaProducerPool(producerFactoryConf,
        Try(globalContextConf.getInt("maxActive")).toOption,
        Try(globalContextConf.getInt("maxIdle")).toOption,
        Try(globalContextConf.getLong("minEvictableIdleTimeMillis")).toOption,
        Try(globalContextConf.getLong("timeBetweenEvictionRunsMillis")).toOption,
        Try(globalContextConf.getBoolean("enableLifo")).getOrElse(true)
      )
    } catch {
      case NonFatal(e) =>
        ConnektLogger(LogFile.FACTORY).error("Failed creating kafka producer pool. %s".format(e.getMessage), e)
        throw e
    }
  }
}

object KafkaProducerHelper {

  private var instance: KafkaProducerHelper = null

  def init(consumerConfig: Config, globalContextConf: Config) = {
    instance = new KafkaProducerHelper(consumerConfig, globalContextConf)
  }
  
  def writeMessages(topic: String, message: String*) = {
    val producer = instance.kafkaProducerPool.borrowObject()
    try {
      val keyedMessages = message.map(new KeyedMessage[String, String](topic, _))
      producer.send(keyedMessages:_*)
    } finally {
      instance.kafkaProducerPool.returnObject(producer)
    }
  }
}