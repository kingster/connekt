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

  def persistCallbackEvent(requestId: String, forContact: String, channel: Channel.Value, callbackEvent: CallbackEvent): Try[String]

  def fetchCallbackEvent(requestId: String, contactId: String, channel: Channel.Value): Try[List[CallbackEvent]]

  def fetchCallbackEventByContactId(contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[List[CallbackEvent]]

  def fetchEventsMapForContactId(contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[Map[String, List[CallbackEvent]]]

  def fetchCallbackEventByMId(messageId: String, channel: Channel.Value): Try[Map[String, List[CallbackEvent]]]

}
