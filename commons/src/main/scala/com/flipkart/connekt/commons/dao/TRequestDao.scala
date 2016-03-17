/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ChannelRequestInfo, ConnektRequest}

trait TRequestDao extends Dao {
  def saveRequest(requestId: String, request: ConnektRequest)
  def fetchRequest(connektId: String): Option[ConnektRequest]
  def updateRequestStatus(id: String, status: ChannelRequestData)
  def fetchRequestInfo(id: String): Option[ChannelRequestInfo]
}
