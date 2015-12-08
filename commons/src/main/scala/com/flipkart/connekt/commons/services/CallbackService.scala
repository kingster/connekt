package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.{EmailCallbackDao, PNCallbackDao}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.CallbackEvent

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

  override def persistCallbackEvent(requestId: String, channel: String, callbackEvent: CallbackEvent): Try[String] = {
    try {
      channelEventsDao(channel).saveCallbackEvent(requestId, callbackEvent)
      ConnektLogger(LogFile.SERVICE).info(s"Event saved for $requestId")
      Success(requestId)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed saving event for $requestId, ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def fetchCallbackEvent(requestId: String, channel: String): Try[Option[CallbackEvent]] = {
    try {
      Success(channelEventsDao(channel).fetchCallbackEvent(requestId))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed fetching event for $requestId, ${e.getMessage}", e)
        Failure(e)
    }
  }
}
