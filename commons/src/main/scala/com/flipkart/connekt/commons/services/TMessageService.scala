package com.flipkart.connekt.commons.services

import java.util.UUID

import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest}

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
trait TMessageService extends TService {
  protected def generateId: String = UUID.randomUUID().toString
  def persistRequest(request: ConnektRequest, requestBucket: String, isCrucial: Boolean = true): Try[String]
  protected def enqueueRequest(request: ConnektRequest, requestBucket: String)
  def getRequestInfo(id: String): Try[Option[ConnektRequest]]
  def updateRequestStatus(id: String, channelStatus: ChannelRequestData): Try[String]
  def saveFetchRequest(request: ConnektRequest, isCrucial: Boolean): Try[String]
  def getFetchRequest(subscriberId: String, minTimestamp: Long, maxTimestamp: Long): Try[List[ConnektRequest]]
}
