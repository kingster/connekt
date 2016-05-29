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
package com.flipkart.connekt.barklice.di

import java.io.IOException
import java.util.Properties

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.metrics.MetricRegistry
import com.flipkart.connekt.commons.services.ConnektConfig
import com.google.inject.AbstractModule
import flipkart.cp.convert.chronosQ.core.{SchedulerCheckpointer, SchedulerSink}
import flipkart.cp.convert.chronosQ.exceptions.SchedulerException
import flipkart.cp.convert.chronosQ.impl.hbase.{HbaseSchedulerCheckpoint, HbaseSchedulerStore}
import flipkart.cp.convert.chronosQ.impl.kafka.{KafkaMessage, KafkaSchedulerSink}
import kafka.producer.{KeyedMessage, ProducerConfig}
import org.apache.curator.RetryPolicy
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry

import scala.util.Try

abstract class ConnektSchedulerModule extends AbstractModule {

  protected var appName: String = null
  protected var numPartitions: Int = 0

  protected def initializeClassMembers()

  protected def configureTaskList()

  protected def configureWorkerTaskFactory()

  protected def configure() {
    initializeClassMembers()
    configureTaskList()
    configureClient()
    configureMetricRegistry()
    configureWorkerTaskFactory()
  }

  protected def configureClient() {

    val zookeeperHost = ConnektConfig.getString("connections.scheduler.worker.zookeeper.host").get
    val baseSleepInMillis  = ConnektConfig.getInt("scheduler.worker.baseSleepInMilliSecs").getOrElse(10000)
    val maxRetryCount = ConnektConfig.getInt("scheduler.worker.maxRetryCount").getOrElse(5)
    val zookeeperSessionTimeoutInMillis = ConnektConfig.getInt("connections.scheduler.worker.zookeeper.sessionTimeoutInMillis").getOrElse(10000)
    val zookeeperConnectionTimeoutInMillis = ConnektConfig.getInt("connections.scheduler.worker.zookeeper.connectionTimeoutInMillis").getOrElse(60000)
    val retryPolicy: RetryPolicy = new ExponentialBackoffRetry(baseSleepInMillis, maxRetryCount)
    val curatorClient = CuratorFrameworkFactory.newClient(zookeeperHost, zookeeperSessionTimeoutInMillis, zookeeperConnectionTimeoutInMillis, retryPolicy)
    curatorClient.start()
    bind(classOf[CuratorFramework]).toInstance(curatorClient)
  }

  protected def configureMetricRegistry() {
    bind(classOf[com.codahale.metrics.MetricRegistry]).toInstance(MetricRegistry.REGISTRY)
  }


  @throws(classOf[IOException])
  protected def configureSink: SchedulerSink = {
    val props: Properties = new Properties()
    props.put("metadata.broker.list", ConnektConfig.getString("connections.kafka.producerConnProps.metadata.broker.list").get)
    props.put("producer.type", "sync")
    props.put("serializer.class", "kafka.serializer.StringEncoder")

    val producerConfig = new ProducerConfig(props)

    new KafkaSchedulerSink(producerConfig, "invalid_topic_since_topic_is_derived_from_value", new KafkaMessage() {
      override def getKeyedMessage(topic: String, value: String): KeyedMessage[String, String] = {
        val keyValue = value.split("\\$", 2)
        new KeyedMessage[String, String](keyValue(0), keyValue(1))
      }
    })
  }

  protected def configureStore: HbaseSchedulerStore = {
    new HbaseSchedulerStore(DaoFactory.getHTableFactory.getConnection,
      ConnektConfig.get("tables.hbase.scheduler.store").get,
      ConnektConfig.getOrElse("scheduler.hbase.store.columnFamily", "d"), appName)
  }

  @throws(classOf[SchedulerException])
  protected def configureCheckPoint: SchedulerCheckpointer = {
    lazy val resumeCheckpointSince = ConnektConfig.getDouble("scheduler.worker.resumeCheckpointSince").getOrElse(System.currentTimeMillis().toDouble).toLong
    val schedulerCheckPointer = new HbaseSchedulerCheckpoint(
      DaoFactory.getHTableFactory.getConnection,
      ConnektConfig.get("tables.hbase.scheduler.checkpointer").get,
      ConnektConfig.getOrElse("scheduler.hbase.checkpoint.columnFamily", "d"), appName
    )

    (0 until numPartitions).foreach(i => {
      val previousCheckpoint = Try(schedulerCheckPointer.peek(i)).recover {
          case e: SchedulerException =>
            ConnektLogger(LogFile.WORKERS).error(s"No current checkpoint for partition $i", e)
            null
        }.get
      if (null == previousCheckpoint)
        schedulerCheckPointer.set(String.valueOf(resumeCheckpointSince), i)
    })
    schedulerCheckPointer
  }
}
