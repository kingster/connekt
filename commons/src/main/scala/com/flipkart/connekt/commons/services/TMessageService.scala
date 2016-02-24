package com.flipkart.connekt.commons.services

import java.util.UUID

import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.entities.Channel.Channel
import com.flipkart.connekt.commons.iomodels.ConnektRequest

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
trait TMessageService extends TService {

  protected def generateId: String = UUID.randomUUID().toString

  def saveRequest(request: ConnektRequest, requestBucket: String, isCrucial: Boolean = true): Try[String]

  def getRequestBucket(request: ConnektRequest, client: AppUser): String

  def assignClientChannelTopic(channel: Channel, clientUserId: String): String

  def getClientChannelTopic(channel: Channel, clientUserId: String): String

  def enqueueRequest(request: ConnektRequest, requestBucket: String)

  def getRequestInfo(id: String): Try[Option[ConnektRequest]]

  def addClientTopic(topicName: String, numPartitions: Int, replicationFactor: Int = 1): Try[Unit]

  def partitionEstimate(qpsBound: Int): Int

  def getTopicNames(channel: Channel): Try[Seq[String]]
}
