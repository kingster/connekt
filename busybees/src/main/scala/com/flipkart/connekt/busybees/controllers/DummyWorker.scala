package com.flipkart.connekt.busybees.controllers

import java.util.Properties
import java.util.concurrent.TimeUnit

import com.flipkart.connekt.busybees.clients.GCMClient
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaConnectionHelper
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.services.CredentialManager
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.ConfigFactory

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
class DummyWorker extends Runnable with KafkaConnectionHelper {

  lazy val kafkaConsumerPool = createKafkaConsumerFactory
  override def run() = {
    println("run invoke at" +System.currentTimeMillis)
    val consumer = kafkaConsumerPool.borrowObject()
    try {
      val streams = consumer.createMessageStreams(Map[String, Int]("fk-connekt-pn" -> 1))
      streams.keys.foreach(topic => {
        streams.get(topic).map(_.zipWithIndex).foreach(l => {
          ConnektLogger(LogFile.WORKERS).info(s"Reading streams for topic: $topic")
          l.foreach(x => {
            val streamIterator = x._1.iterator()
            while (streamIterator.hasNext()) {
              val msg = streamIterator.next()
              ConnektLogger(LogFile.WORKERS).debug(s"stream: ${x._2} message: ${msg.message}")

              val request = new String(msg.message).getObj[ConnektRequest]
              val pnData = request.channelInfo.asInstanceOf[PNRequestData]
              val pnInfo = request.channelData.asInstanceOf[PNRequestInfo]
              ConnektLogger(LogFile.WORKERS).info(s"PN Request: ${pnData.getJson}")
              GCMClient.instance.wirePN(request.id, pnInfo, pnData, CredentialManager.getCredential("PN.ConnektSampleApp").password)
            }
          })
        })
      })
    } finally {
      kafkaConsumerPool.returnObject(consumer)
    }
  }

  def createKafkaConsumerFactory = {
    val consumerConnProps = new Properties()
    consumerConnProps.setProperty("zookeeper.connect", "10.32.197.54:2181,10.32.165.183:2181,10.32.237.188:2181/kafka/preprod6" )
    consumerConnProps.setProperty("group.id", "a")
    consumerConnProps.setProperty("zookeeper.session.timeout.ms", "5000")
    consumerConnProps.setProperty("zookeeper.sync.time.ms", "200")
    consumerConnProps.setProperty("auto.commit.interval.ms", "1000")

    val consumerFactoryConf = ConfigFactory.parseProperties(consumerConnProps)
    createKafkaConsumerPool(consumerFactoryConf, Some(5), Some(1), Some(1000L * 60L * 30L), Some(-1), enableLifo = false)
  }
}

object DummyWorker {
  lazy val tPE = new java.util.concurrent.ScheduledThreadPoolExecutor(5)
  lazy val workable = new DummyWorker

  def init() = try {
    ConnektLogger(LogFile.WORKERS).info("DummyWorkers Init.")
    tPE.scheduleAtFixedRate(workable, 0, 100, TimeUnit.MILLISECONDS)
  } catch {
    case e: Exception =>
      ConnektLogger(LogFile.SERVICE).error(s"worker exec failed ${e.getMessage}", e)
  }

}