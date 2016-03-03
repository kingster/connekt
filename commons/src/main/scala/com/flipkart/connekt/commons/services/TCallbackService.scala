package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.iomodels.CallbackEvent

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
trait TCallbackService extends TService {

  def persistCallbackEvent(appName: String, requestId: String, forContact: String, channel: Channel.Value, callbackEvent: CallbackEvent): Try[String]

  def fetchCallbackEvent(appName: String, requestId: String, contactId: String, channel: Channel.Value): Try[List[CallbackEvent]]

  def fetchCallbackEventByContactId(appName: String, contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[List[CallbackEvent]]

  def fetchEventsMapForContactId(appName: String, contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[Map[String, List[CallbackEvent]]]

  def fetchCallbackEventByMId(appName: String, messageId: String, channel: Channel.Value): Try[Map[String, List[CallbackEvent]]]

}
