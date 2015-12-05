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
        ConnektLogger(LogFile.FACTORY).error("Failed creating kafka consumer pool. %s".format(e.getMessage), e)
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
    case e: Exception => ConnektLogger(LogFile.FACTORY).error("Failed returning kafkaConnector." + e.getMessage, e)
  }
}

object KafkaConsumerHelper {

  var instance: KafkaConsumerHelper = null

  def init(consumerConfig: Config, globalContextConf: Config) = {
    if (null != instance)
      this.synchronized {
        instance = KafkaConsumerHelper(consumerConfig, globalContextConf)
      }
    instance
  }

  def shutdown() = try {
    instance.kafkaConsumerPool.close()
  } catch {
    case e: Exception => ConnektLogger(LogFile.FACTORY).error("Error in KafkaConsumerHelper companion shutdown. " + e.getMessage, e)
  }

  def apply(consumerConfig: Config, globalContextConf: Config) =
    new KafkaConsumerHelper(consumerConfig, globalContextConf)

  def readMessage(topic: String): Option[String] = {
    lazy val streamsMap = scala.collection.mutable.Map[String, KafkaStream[Array[Byte], Array[Byte]]]()

    lazy val consumerStream: Option[KafkaStream[Array[Byte], Array[Byte]]] = streamsMap.get(topic).orElse({
      val s = instance.kafkaConsumerPool.borrowObject().createMessageStreams(Map[String, Int](topic -> 1)).get(topic).get.headOption
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
