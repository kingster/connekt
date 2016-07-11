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
package com.flipkart.connekt.commons.helpers

import java.util.Properties

import com.flipkart.connekt.commons.factories.{KafkaConsumerFactory, KafkaProducerFactory}
import com.flipkart.connekt.commons.utils.StringUtils
import com.typesafe.config.Config
import kafka.consumer.{Consumer, ConsumerConfig}
import kafka.producer.{ProducerConfig, Producer}
import kafka.utils.{ZKGroupTopicDirs, ZkUtils, ZKStringSerializer}
import org.I0Itec.zkclient.ZkClient
import org.apache.commons.pool.impl.GenericObjectPool

import scala.util.Try
import StringUtils._

trait KafkaConnectionHelper {

  def createKafkaConsumerPool(factoryConf: Config,
                              maxActive: Option[Int],
                              maxIdle: Option[Int],
                              minEvictionIdleMillis: Option[Long],
                              timeBetweenEvictionRunMillis: Option[Long],
                              enableLifo: Boolean = false) = {

    val factoryProps = new Properties()
    factoryProps.setProperty("zookeeper.connect", factoryConf.getString("zookeeper.connect"))
    factoryProps.setProperty("group.id", factoryConf.getString("group.id"))
    factoryProps.setProperty("zookeeper.session.timeout.ms", factoryConf.getString("zookeeper.session.timeout.ms"))
    factoryProps.setProperty("zookeeper.sync.time.ms", factoryConf.getString("zookeeper.sync.time.ms"))
    factoryProps.setProperty("auto.commit.interval.ms", factoryConf.getString("auto.commit.interval.ms"))
    factoryProps.setProperty("consumer.timeout.ms", factoryConf.getString("consumer.timeout.ms"))

    val kafkaConsumerFactory = new KafkaConsumerFactory(factoryProps)

    val kafkaConsumerPool = new GenericObjectPool(kafkaConsumerFactory)
    kafkaConsumerPool.setMaxActive(maxActive.getOrElse(GenericObjectPool.DEFAULT_MAX_ACTIVE))
    kafkaConsumerPool.setMaxIdle(maxIdle.getOrElse(GenericObjectPool.DEFAULT_MAX_IDLE))
    kafkaConsumerPool.setMinEvictableIdleTimeMillis(minEvictionIdleMillis.getOrElse(GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS))
    kafkaConsumerPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunMillis.getOrElse(GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS))
    kafkaConsumerPool.setWhenExhaustedAction(GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION)
    kafkaConsumerPool.setLifo(enableLifo)

    kafkaConsumerPool
  }

  def createKafkaProducerPool(factoryConf: Config,
                              maxActive: Option[Int],
                              maxIdle: Option[Int],
                              minEvictionIdleMillis: Option[Long],
                              timeBetweenEvictionRunMillis: Option[Long],
                              enableLifo: Boolean = false) = {

    val factoryProps = new Properties()
    factoryProps.setProperty("metadata.broker.list", factoryConf.getString("metadata.broker.list"))
    factoryProps.setProperty("serializer.class", factoryConf.getString("serializer.class"))
    factoryProps.setProperty("request.required.acks", factoryConf.getString("request.required.acks"))
    factoryProps.setProperty("producer.type", Try(factoryConf.getString("producer.type")).getOrElse("sync"))

    val kafkaProducerFactory = new KafkaProducerFactory[String, String](factoryProps)

    val kafkaProducerPool = new GenericObjectPool(kafkaProducerFactory)
    kafkaProducerPool.setMaxActive(maxActive.getOrElse(GenericObjectPool.DEFAULT_MAX_ACTIVE))
    kafkaProducerPool.setMaxIdle(maxIdle.getOrElse(GenericObjectPool.DEFAULT_MAX_IDLE))
    kafkaProducerPool.setMinEvictableIdleTimeMillis(minEvictionIdleMillis.getOrElse(GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS))
    kafkaProducerPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunMillis.getOrElse(GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS))
    kafkaProducerPool.setWhenExhaustedAction(GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION)
    kafkaProducerPool.setLifo(enableLifo)

    kafkaProducerPool
  }
  
  def createKafkaConsumer(groupId: String, kafkaConsumerConf: Config) = {
    
    require(groupId.isDefined, "`groupId` must be defined")
    
    val consumerProps = new Properties()
    consumerProps.setProperty("zookeeper.connect", kafkaConsumerConf.getString("zookeeper.connect"))
    consumerProps.setProperty("group.id", groupId)
    consumerProps.setProperty("zookeeper.session.timeout.ms", kafkaConsumerConf.getString("zookeeper.session.timeout.ms"))
    consumerProps.setProperty("zookeeper.sync.time.ms", kafkaConsumerConf.getString("zookeeper.sync.time.ms"))
    consumerProps.setProperty("auto.commit.interval.ms", kafkaConsumerConf.getString("auto.commit.interval.ms"))
    consumerProps.setProperty("consumer.timeout.ms", kafkaConsumerConf.getString("consumer.timeout.ms"))
    
    Consumer.create(new ConsumerConfig(consumerProps))
  }
  
  def createKafkaProducer[K, M](kafkaProducerConf: Config) = {

    val producerProps = new Properties()
    producerProps.setProperty("metadata.broker.list", kafkaProducerConf.getString("metadata.broker.list"))
    producerProps.setProperty("serializer.class", kafkaProducerConf.getString("serializer.class"))
    producerProps.setProperty("request.required.acks", kafkaProducerConf.getString("request.required.acks"))
    producerProps.setProperty("producer.type", Try(kafkaProducerConf.getString("producer.type")).getOrElse("sync"))

    new Producer[K, M](new ProducerConfig(producerProps))
  }

  def offsets(topic: String, groupId: String, zkPath: String): Map[Int, (Long, String)] = {
    val zkClient = new ZkClient(zkPath, 5000, 5000, ZKStringSerializer)
    val partitions = ZkUtils.getPartitionsForTopics(zkClient, List(topic))

    partitions.flatMap(topicAndPart => {
      val topicDirs = new ZKGroupTopicDirs(groupId, topic)
      topicAndPart._2.map(partitionId => {
        val zkPath = s"${topicDirs.consumerOffsetDir}/$partitionId"
        val ownerPath = s"${topicDirs.consumerOwnerDir}/$partitionId"
        val owner = ZkUtils.readDataMaybeNull(zkClient, ownerPath)._1.getOrElse("No owner")
        val checkPoint = ZkUtils.readDataMaybeNull(zkClient, zkPath)._1.map(_.toLong).getOrElse(0L)
        partitionId -> (checkPoint, owner)
      })
    }).toMap
  }

  def zkPath(kafkaConfig: Config) = kafkaConfig.getString("zookeeper.connect")
}
