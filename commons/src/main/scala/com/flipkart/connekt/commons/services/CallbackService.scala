package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import org.apache.commons.lang.RandomStringUtils

import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
class CallbackService (pnEventsDao: PNCallbackDao,emailEventsDao: EmailCallbackDao, pnRequestDao: PNRequestDao, emailRequestDao: EmailRequestDao) extends TCallbackService {


  private def channelEventsDao(channel: String) = channel match {
    case "push" => pnEventsDao
    case "email" => emailEventsDao
  }

  private def requestDao(channel: String) = channel match {
    case "push" => pnRequestDao
    case "email" => emailRequestDao
  }


  override def persistCallbackEvent(requestId: String, forContact: String,  channel: String, callbackEvent: CallbackEvent): Try[String] = {
    try {

      channelEventsDao(channel).saveCallbackEvent(requestId, forContact, nextEventId(), callbackEvent)
      ConnektLogger(LogFile.SERVICE).debug(s"Event saved for $requestId")
      Success(requestId)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed saving event for $requestId, ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def fetchCallbackEvent(requestId: String, contactId: String, channel: String): Try[List[CallbackEvent]] = {
    try {
      Success(channelEventsDao(channel).fetchCallbackEvents(requestId, contactId))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed fetching event for $requestId, ${e.getMessage}", e)
        Failure(e)
    }
  }

  private def nextEventId() = RandomStringUtils.randomAlphabetic(10)

  def fetchCallbackEventByContactId(contactId: String, channel: String): Try[List[CallbackEvent]] = {
    try {
      Success(channelEventsDao(channel).fetchCallbackEvents("", contactId))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed fetching event for $contactId, ${e.getMessage}", e)
        Failure(e)
    }
  }

  def fetchCallbackEventByMId(messageId: String, channel: String): Try[List[CallbackEvent]] = {
    try {
      val events = requestDao(channel).fetchRequestInfo(messageId)
      Success(channelEventsDao(channel).fetchCallbackEvents(messageId, events.get))

    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed fetching event for $messageId, ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def fetchEventsMapForContactId(contactId: String, channel: String): Try[Map[String, List[CallbackEvent]]] = {
    try {
      val eventList = fetchCallbackEventByContactId(contactId, channel)
      Success(channelEventsDao(channel).fetchEventMapFromList(eventList.get))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed fetching event for $contactId, ${e.getMessage}", e)
        Failure(e)
    }
  }
}
