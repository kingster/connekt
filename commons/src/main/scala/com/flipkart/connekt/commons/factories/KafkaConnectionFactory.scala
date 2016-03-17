/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.factories

import java.util.Properties

import kafka.consumer.{Consumer, ConsumerConfig, ConsumerConnector}
import kafka.producer.{Producer, ProducerConfig}
import org.apache.commons.pool.PoolableObjectFactory

class KafkaProducerFactory[K, M](producerProps: Properties) extends PoolableObjectFactory[Producer[K, M]] {

  val config: ProducerConfig = new ProducerConfig(producerProps)

  override def makeObject(): Producer[K, M] = new Producer[K, M](config)

  override def destroyObject(obj: Producer[K, M]): Unit = obj.close()

  override def validateObject(obj: Producer[K, M]): Boolean = true

  override def activateObject(obj: Producer[K, M]): Unit = {}

  override def passivateObject(obj: Producer[K, M]): Unit = {}
}

class KafkaConsumerFactory(consumerProps: Properties) extends PoolableObjectFactory[ConsumerConnector] {

  val config: ConsumerConfig = new ConsumerConfig(consumerProps)

  override def makeObject(): ConsumerConnector = Consumer.create(config)

  override def destroyObject(obj: ConsumerConnector): Unit = obj.shutdown()

  override def validateObject(obj: ConsumerConnector): Boolean = true

  override def activateObject(obj: ConsumerConnector): Unit = {}

  override def passivateObject(obj: ConsumerConnector): Unit = {}
}
