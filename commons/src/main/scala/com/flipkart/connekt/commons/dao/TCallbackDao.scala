package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.iomodels.CallbackEvent

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
trait TCallbackDao extends Dao {
  def saveCallbackEvent(appName:String, requestId: String, forContact: String, eventId: String, callbackEvent: CallbackEvent): Try[String]
  def fetchCallbackEvents(appName: String, requestId: String, forContact: String, timestampRange: Option[(Long, Long)]): List[CallbackEvent]
}
