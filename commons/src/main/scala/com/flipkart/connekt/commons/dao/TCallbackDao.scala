/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.iomodels.CallbackEvent

import scala.util.Try

trait TCallbackDao extends Dao {
  def saveCallbackEvent(requestId: String, forContact: String, eventId: String, callbackEvent: CallbackEvent): Try[String]

  def fetchCallbackEvents(requestId: String, forContact: String, timestampRange: Option[(Long, Long)], maxRowsLimit: Option[Int] = None): List[CallbackEvent]
}
