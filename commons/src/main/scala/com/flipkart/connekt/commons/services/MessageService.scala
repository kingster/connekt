package com.flipkart.connekt.commons.services

import java.util.Properties

import com.flipkart.connekt.commons.dao.TRequestDao
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.{KafkaConsumer, KafkaProducerHelper}
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.utils.StringUtils._
import kafka.utils.ZKStringSerializer
import org.I0Itec.zkclient.ZkClient
import com.roundeights.hasher.Implicits._
import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
class MessageService(requestDao: TRequestDao, queueProducerHelper: KafkaProducerHelper, queueConsumerHelper: KafkaConsumer) extends TMessageService {

  private val messageDao: TRequestDao = requestDao
  private val queueProducer: KafkaProducerHelper = queueProducerHelper
  private val clientRequestTopics = scala.collection.mutable.Map[String, String]()

  override def saveRequest(request: ConnektRequest, requestBucket: String, isCrucial: Boolean): Try[String] = {
    try {
      val reqWithId = request.copy(id = generateId)
      messageDao.saveRequest(reqWithId.id, reqWithId)
      queueProducer.writeMessages(requestBucket, reqWithId.getJson)
      ConnektLogger(LogFile.SERVICE).info(s"Saved request ${reqWithId.id} to $requestBucket")
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
      Success(requestDao.fetchRequest(id))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Get request info failed ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def getRequestBucket(request: ConnektRequest, client: AppUser): String = {
    getClientChannelTopic(request.channel, client.userId)
  }

  override def getClientChannelTopic(channel: String, clientUserId: String): String = {
    clientRequestTopics.getOrElseUpdate(s"$clientUserId#$channel", s"${channel}_${clientUserId.sha256.hash.hex}")
  }

  //# ADMIN ACTIONS
  override def addClientTopic(topicName: String, numPartitions: Int, replicationFactor: Int = 1): Try[Unit] = Try {
    val zkClient = new ZkClient(queueProducerHelper.zkPath, 5000, 5000, ZKStringSerializer)
    kafka.admin.AdminUtils.createTopic(zkClient, topicName, numPartitions, replicationFactor, new Properties())
    ConnektLogger(LogFile.SERVICE).info(s"Created topic $topicName with $numPartitions, replicationFactor $replicationFactor")
  }

  override def partitionEstimate(qpsBound: Int): Int = {
    ConnektConfig.getInt("admin.partitionsPer5k").getOrElse(1) * Math.max(qpsBound / 5000, 1)
  }
}
