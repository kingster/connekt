package com.flipkart.connekt.commons.helpers

import java.util.Properties

import com.flipkart.connekt.commons.factories.{KafkaConsumerFactory, KafkaProducerFactory}
import com.typesafe.config.Config
import org.apache.commons.pool.impl.GenericObjectPool

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 11/15/15
 */
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
}
