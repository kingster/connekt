package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.iomodels.CallbackEvent

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
trait TCallbackService extends TService {
  def persistCallbackEvent(requestId: String, forContact: String, channel: String, callbackEvent: CallbackEvent): Try[String]
  def fetchCallbackEvent(requestId: String, contactId: String, channel: String): Try[List[CallbackEvent]]
  def fetchCallbackEventByContactId(contactId: String, channel: String): Try[List[CallbackEvent]]
  def fetchEventsMapForContactId(contactId: String, channel: String): Try[Map[String, List[CallbackEvent]]]
  def fetchCallbackEventByMId(messageId: String, channel: String): Try[List[CallbackEvent]]
}
