/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao._
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.metrics.Timed
import com.flipkart.connekt.commons.core.Wrappers._

import scala.util.Try

class CallbackService(pnEventsDao: PNCallbackDao, emailEventsDao: EmailCallbackDao, pnRequestDao: PNRequestDao, emailRequestDao: EmailRequestDao,  queueProducerHelper: KafkaProducerHelper) extends TCallbackService with Instrumented {

  lazy val MAX_FETCH_EVENTS = ConnektConfig.get("receptors.callback.events.max-results").orElse(Some(100))
  lazy val CALLBACK_QUEUE_NAME = ConnektConfig.get("firefly.kafka.topic").getOrElse("ckt_callback_events")

  private def channelEventsDao(channel: Channel.Value) = channel match {
    case Channel.PUSH => pnEventsDao
    case Channel.EMAIL => emailEventsDao
  }

  private def requestDao(channel: Channel.Value) = channel match {
    case Channel.PUSH => pnRequestDao
    case Channel.EMAIL => emailRequestDao
  }

  @Timed("persistCallbackEvent")
  override def persistCallbackEvents(channel: Channel.Value, events: List[CallbackEvent]): Try[List[String]] = {
    Try {
      val rowKeys = channelEventsDao(channel).asyncSaveCallbackEvents(events)
      enqueueCallbackEvents(events).get
      ConnektLogger(LogFile.SERVICE).debug(s"Event saved with rowKeys $rowKeys")
      rowKeys
    }
  }

  lazy val producer = queueProducerHelper.kafkaProducerPool.borrowObject()


  @Timed("enqueueCallbackEvent")
  def enqueueCallbackEvents(events: List[CallbackEvent]): Try[Unit] = Try_ {
    queueProducerHelper.writeMessages(CALLBACK_QUEUE_NAME, events.map(_.getJson) : _*)
  }

  @Timed("fetchCallbackEvent")
  override def fetchCallbackEvent(requestId: String, contactId: String, channel: Channel.Value): Try[List[(CallbackEvent, Long)]] = {
    Try {
      channelEventsDao(channel).fetchCallbackEvents(requestId, contactId, None, MAX_FETCH_EVENTS)
    }
  }

  @Timed("fetchCallbackEventByContactId")
  def fetchCallbackEventByContactId(contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[List[(CallbackEvent, Long)]] = {
    Try {
      channelEventsDao(channel).fetchCallbackEvents("", contactId, Some(Tuple2(minTimestamp, maxTimestamp)), MAX_FETCH_EVENTS)
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

  @Timed("deleteCallBackEvent")
  def deleteCallBackEvent(requestId: String, forContact: String, channel: Channel.Value): Try[List[CallbackEvent]] = {
    Try {
      channelEventsDao(channel).deleteCallbackEvents(requestId, forContact)
    }
  }

  @Timed("fetchEventsMapForContactId")
  override def fetchEventsMapForContactId(contactId: String, channel: Channel.Value, minTimestamp: Long, maxTimestamp: Long): Try[Map[String, List[CallbackEvent]]] = {
    Try {
      val eventList = fetchCallbackEventByContactId(contactId, channel, minTimestamp, maxTimestamp)
      channelEventsDao(channel).fetchEventMapFromList(eventList.get.map(_._1))
    }
  }
}
