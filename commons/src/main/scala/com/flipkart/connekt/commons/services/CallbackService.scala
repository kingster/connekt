package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.{EmailCallbackDao, PNCallbackDao}
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
class CallbackService extends TCallbackService {
  var pnEventsDao: PNCallbackDao = null
  var emailEventsDao: EmailCallbackDao = null

  def withPNEventsPersistence(pnCallbackEventsDao: PNCallbackDao) = {
    pnEventsDao = pnCallbackEventsDao
    this
  }

  def withEmailEventsPersistence(emailCallbackEventsDao: EmailCallbackDao) = {
    emailEventsDao = emailCallbackEventsDao
    this
  }

  private def channelEventsDao(channel: String) = channel match {
    case "push" => pnEventsDao
    case "email" => emailEventsDao
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
}
