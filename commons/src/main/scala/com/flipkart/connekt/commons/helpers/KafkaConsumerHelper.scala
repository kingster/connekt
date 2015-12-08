package com.flipkart.connekt.commons.helpers

import java.util.NoSuchElementException

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.typesafe.config.Config
import kafka.consumer.{ConsumerConnector, KafkaStream}
import org.apache.commons.pool.impl.GenericObjectPool

import scala.util.{Success, Failure, Try}
import scala.util.control.NonFatal

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
trait KafkaConsumer {
  def readMessage(topic: String): Option[String]
}

class KafkaConsumerHelper (val consumerFactoryConf: Config, globalContextConf: Config) extends KafkaConnectionHelper with GenericObjectPoolHelper {

  validatePoolProps("kafka consumer pool", globalContextConf)

  val kafkaConsumerPool: GenericObjectPool[ConsumerConnector] = {
    try {
      createKafkaConsumerPool(consumerFactoryConf,
        Try(globalContextConf.getInt("maxActive")).toOption,
        Try(globalContextConf.getInt("maxIdle")).toOption,
        Try(globalContextConf.getLong("minEvictableIdleTimeMillis")).toOption,
        Try(globalContextConf.getLong("timeBetweenEvictionRunsMillis")).toOption,
        Try(globalContextConf.getBoolean("enableLifo")).getOrElse(true)
      )
    } catch {
      case NonFatal(e) =>
        ConnektLogger(LogFile.FACTORY).error(s"Failed creating kafka consumer pool. ${e.getMessage}", e)
        throw e
    }
  }

  def getConnector = Try[ConsumerConnector](kafkaConsumerPool.borrowObject()) match {
    case Failure(e) => e match {
      case q: NullPointerException => throw new RuntimeException("kafka consumer helper un-initialized." + q.getMessage, q)
      case a: NoSuchElementException => throw new Exception("kafka pool exhausted." + a.getMessage, a)
      case z: Exception => throw z
    }
    case Success(cC) => cC
  }

  def returnConnector(cC: ConsumerConnector) = try {
    kafkaConsumerPool.returnObject(cC)
  } catch {
    case e: Exception => ConnektLogger(LogFile.FACTORY).error(s"Failed returning kafkaConnector. ${e.getMessage}", e)
  }

  def shutdown() = try {
    kafkaConsumerPool.close()
    ConnektLogger(LogFile.FACTORY).info("KafkaProducerHelper shutdown.")
  } catch {
    case e: Exception => ConnektLogger(LogFile.FACTORY).error(s"Error in KafkaConsumerHelper companion shutdown. ${e.getMessage}", e)
  }

  def readMessage(topic: String): Option[String] = {
    lazy val streamsMap = scala.collection.mutable.Map[String, KafkaStream[Array[Byte], Array[Byte]]]()

    lazy val consumerStream: Option[KafkaStream[Array[Byte], Array[Byte]]] = streamsMap.get(topic).orElse({
      val s = kafkaConsumerPool.borrowObject().createMessageStreams(Map[String, Int](topic -> 1)).get(topic).get.headOption
      s.foreach(streamsMap += topic -> _)
      s
    })

    if(consumerStream.isDefined) {
      val i = consumerStream.get.iterator()
      if(i.hasNext()) Some(new String(i.next.message))
    }
    None
  }
}

object KafkaConsumerHelper extends KafkaConsumer {

  var instance: KafkaConsumerHelper = null

  def apply(consumerConfig: Config, globalContextConf: Config) =
    new KafkaConsumerHelper(consumerConfig, globalContextConf)

  def init(consumerConfig: Config, globalContextConf: Config) = {
    if (null != instance)
      this.synchronized {
        instance = KafkaConsumerHelper(consumerConfig, globalContextConf)
      }
    instance
  }

  def shutdown() = instance.shutdown()

  def readMessage(topic: String): Option[String] = instance.readMessage(topic)
}
