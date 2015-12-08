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
  def persistRequest(request: ConnektRequest, isCrucial: Boolean = true): Option[String]
  protected def enqueueRequest(request: ConnektRequest)
  def getRequestInfo(id: String): Option[ConnektRequest]
}
