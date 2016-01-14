package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.TRequestDao
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.{KafkaConsumer, KafkaProducer}
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
class IMessageService(requestDao: TRequestDao, queueProducerHelper: KafkaProducer, queueConsumerHelper: KafkaConsumer) extends TMessageService {

  private val messageDao: TRequestDao = requestDao
  private val queueProducer: KafkaProducer = queueProducerHelper
  private val queueConsumer: KafkaConsumer = queueConsumerHelper

  override def persistRequest(request: ConnektRequest, requestBucket: String, isCrucial: Boolean): Try[String] = {
    try {
      val reqWithId = request.copy(id = generateId)
      queueProducer.writeMessages(requestBucket, reqWithId.getJson)
      messageDao.saveRequest(reqWithId.id, reqWithId)
      ConnektLogger(LogFile.SERVICE).info(s"Persisted request ${reqWithId.id}, with bucket $requestBucket")
      Success(reqWithId.id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Request persistence failed ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def saveFetchRequest(request: ConnektRequest, isCrucial: Boolean): Try[String] = {
    try {
      val requestWithId = request.copy(id = generateId)
      messageDao.savePullRequest(requestWithId.id, requestWithId)
      ConnektLogger(LogFile.SERVICE).info(s"Persisted fetch request ${requestWithId.id}")
      Success(requestWithId.id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"FetchRequest persistence failed ${e.getMessage}", e)
        Failure(e)
    }
  }

  override protected def enqueueRequest(request: ConnektRequest, requestBucket: String): Unit = {
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

  override def updateRequestStatus(id: String, channelStatus: ChannelRequestData): Try[String] = {
    try {
      requestDao.updateRequestStatus(id, channelStatus)
      ConnektLogger(LogFile.SERVICE).info(s"Request status updated $id")
      Success(id)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Update request $id, ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def getFetchRequest(subscriberId: String, minTimestamp: Long, maxTimestamp: Long): Try[List[ConnektRequest]] = {
    try {
      Success(requestDao.fetchPullRequest(subscriberId, minTimestamp, maxTimestamp))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Fetch request failed $subscriberId, ${e.getMessage}", e)
        Failure(e)
    }
  }
}
