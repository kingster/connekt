package com.flipkart.connekt.commons.services

import java.util.UUID

import com.flipkart.connekt.commons.iomodels.ConnektRequest

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
trait MessageService {
  def generateId: String = UUID.randomUUID().toString
  def persistRequest(pnRequest: ConnektRequest, isCrucial: Boolean = true): Option[String]
  protected def enqueueRequest(pnRequest: ConnektRequest)
  def dequeueRequest(): Option[ConnektRequest]
  def getRequestInfo(connektId: String): Option[ConnektRequest]
}
