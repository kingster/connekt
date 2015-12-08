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
  def saveCallbackEvent(requestId: String, callbackEvent: CallbackEvent): Try[String]
  def fetchCallbackEvent(requestId: String): Option[CallbackEvent]
}
