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
package com.flipkart.connekt.commons.services

import java.util.Properties

import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.{TRequestDao, TUserConfiguration}
import com.flipkart.connekt.commons.entities.Channel.Channel
import com.flipkart.connekt.commons.entities.{AppUser, Channel}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.{KafkaConnectionHelper, KafkaProducerHelper}
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.SchedulerService.ScheduledRequest
import com.flipkart.connekt.commons.utils.StringUtils._
import com.roundeights.hasher.Implicits._
import com.typesafe.config.Config
import kafka.utils.{ZKStringSerializer, ZkUtils}
import org.I0Itec.zkclient.ZkClient
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

class MessageService(requestDao: TRequestDao, userConfigurationDao: TUserConfiguration, queueProducerHelper: KafkaProducerHelper, kafkaConsumerConf: Config, schedulerService: SchedulerService) extends TMessageService with KafkaConnectionHelper {

  private val messageDao: TRequestDao = requestDao
  private val queueProducer: KafkaProducerHelper = queueProducerHelper
  private val clientRequestTopics = scala.collection.mutable.Map[String, String]()

  override def saveRequest(request: ConnektRequest, requestBucket: String, isCrucial: Boolean): Try[String] = {
    try {
      val reqWithId = request.copy(id = generateId)
      messageDao.saveRequest(reqWithId.id, reqWithId)

      request.scheduleTs match {
        case Some(scheduleTime) if scheduleTime > System.currentTimeMillis() + 2.minutes.toMillis =>
          schedulerService.client.add(ScheduledRequest(reqWithId, requestBucket), scheduleTime)
          ConnektLogger(LogFile.SERVICE).info(s"Scheduled request ${reqWithId.id} at $scheduleTime to $requestBucket")
        case _ =>
          queueProducer.writeMessages(requestBucket, reqWithId.getJson)
          ConnektLogger(LogFile.SERVICE).info(s"Saved request ${reqWithId.id} to $requestBucket")
      }

      Success(reqWithId.id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Failed to save request ${e.getMessage} to $requestBucket", e)
        Failure(e)
    }
  }

  override def enqueueRequest(request: ConnektRequest, requestBucket: String): Unit = {
    queueProducer.writeMessages(requestBucket, request.getJson)
    ConnektLogger(LogFile.SERVICE).info(s"EnQueued request ${request.id} in bucket $requestBucket")
  }

  override def getRequestInfo(id: String): Try[Option[ConnektRequest]] = {
    try {
      Success(requestDao.fetchRequest(List(id)).headOption)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Get request info failed ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def getRequestInfo(ids: List[String]): Try[List[ConnektRequest]] = {
    try {
      Success(requestDao.fetchRequest(ids))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Get request info failed ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def getRequestBucket(request: ConnektRequest, client: AppUser): String = {
    getClientChannelTopic(Channel.withName(request.channel), client.userId)
  }

  override def getClientChannelTopic(channel: Channel, clientUserId: String): String = {
    clientRequestTopics.getOrElseUpdate(s"$clientUserId#$channel", userConfigurationDao.getUserConfiguration(clientUserId, channel).get.queueName)
  }

  //# ADMIN ACTIONS
  override def addClientTopic(topicName: String, numPartitions: Int, replicationFactor: Int = 1): Try[Unit] = Try_ {
    val zkClient = new ZkClient(zkPath(kafkaConsumerConf), 5000, 5000, ZKStringSerializer)
    kafka.admin.AdminUtils.createTopic(zkClient, topicName, numPartitions, replicationFactor, new Properties())
    ConnektLogger(LogFile.SERVICE).info(s"Created topic $topicName with $numPartitions, replicationFactor $replicationFactor")
  }

  override def partitionEstimate(qpsBound: Int): Int = {
    ConnektConfig.getInt("admin.partitionsPer5k").getOrElse(1) * Math.max(qpsBound / 5000, 1)
  }

  override def getTopicNames(channel: Channel): Try[Seq[String]] = Try_ {
    userConfigurationDao.getAllUserConfiguration(channel).map(_.queueName).intersect(getKafkaTopicNames(channel).get)
  }

  override def assignClientChannelTopic(channel: Channel, clientUserId: String): String = s"${channel}_${clientUserId.md5.hash.hex}"

  override def getKafkaTopicNames(channel: Channel): Try[Seq[String]] = Try_ {
    val allTopics = ZkUtils.getAllTopics(new ZkClient(zkPath(kafkaConsumerConf), 5000, 5000, ZKStringSerializer))
    allTopics.filter(_.startsWith(channel.toString))
  }
}
