package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao._
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.metrics.Timed
import org.apache.commons.lang.RandomStringUtils

import scala.util.{Failure, Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
class CallbackService(pnEventsDao: PNCallbackDao, emailEventsDao: EmailCallbackDao, pnRequestDao: PNRequestDao, emailRequestDao: EmailRequestDao) extends TCallbackService with Instrumented {

  private def channelEventsDao(channel: Channel.Value) = channel match {
    case Channel.PUSH => pnEventsDao
    case Channel.EMAIL => emailEventsDao
  }

  private def requestDao(channel: Channel.Value) = channel match {
    case Channel.PUSH => pnRequestDao
    case Channel.EMAIL => emailRequestDao
  }

  @Timed("persistCallbackEvent")
  override def persistCallbackEvent(requestId: String, forContact: String, channel: Channel.Value, callbackEvent: CallbackEvent): Try[String] = {
    Try {
      channelEventsDao(channel).saveCallbackEvent(requestId, forContact, nextEventId(), callbackEvent)
      ConnektLogger(LogFile.SERVICE).debug(s"Event saved for $requestId")
      requestId
    }
  }

  @Timed("fetchCallbackEvent")
  override def fetchCallbackEvent(requestId: String, contactId: String, channel: Channel.Value): Try[List[CallbackEvent]] = {
    Try {
      channelEventsDao(channel).fetchCallbackEvents(requestId, contactId, None)
    }
  }

  private def nextEventId() = RandomStringUtils.randomAlphabetic(10)

  @Timed("fetchCallbackEventByContactId")
  def fetchCallbackEventByContactId(contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[List[CallbackEvent]] = {
    Try {
      channelEventsDao(channel).fetchCallbackEvents("", contactId, Some(Tuple2(minTimestamp, maxTimestamp)))
    }
  }

  /**
   *
   * @param messageId
   * @param channel
   * @return Map ( DeviceId -> List[Events] )
   */
  @Timed("fetchCallbackEventByMId")
  def fetchCallbackEventByMId(messageId: String, channel: Channel.Value): Try[Map[String, List[CallbackEvent]]] = {
    Try {
      val events = requestDao(channel).fetchRequestInfo(messageId)
      events.isDefined match {
        case true =>
          channelEventsDao(channel).fetchCallbackEvents(messageId, events.get, None)
        case false =>
          Map()
      }
    }
  }

  @Timed("fetchEventsMapForContactId")
  override def fetchEventsMapForContactId(contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[Map[String, List[CallbackEvent]]] = {
    Try {
      val eventList = fetchCallbackEventByContactId(contactId, channel, minTimestamp, maxTimestamp)
      channelEventsDao(channel).fetchEventMapFromList(eventList.get)
    }
  }
}
