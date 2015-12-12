package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest}

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
trait TRequestDao extends Dao {
  def saveRequestInfo(requestId: String, request: ConnektRequest)
  def fetchRequestInfo(connektId: String): Option[ConnektRequest]
  def updateRequestStatus(id: String, status: ChannelRequestData)
}
