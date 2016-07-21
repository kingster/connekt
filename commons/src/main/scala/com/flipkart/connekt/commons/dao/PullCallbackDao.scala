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
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.THTableFactory
import com.flipkart.connekt.commons.iomodels._

class PullCallbackDao(tableName: String, hTableFactory: THTableFactory) extends CallbackDao(tableName: String, hTableFactory: THTableFactory) {

  override def channelEventPropsMap(channelCallbackEvent: CallbackEvent): Map[String, Array[Byte]] = {
    val pullCallbackEvent = channelCallbackEvent.asInstanceOf[PullCallbackEvent]

    Map[String, Array[Byte]](
      "messageId" -> pullCallbackEvent.messageId.getUtf8Bytes,
      "eventId" -> pullCallbackEvent.eventId.getUtf8Bytes,
      "clientId" -> pullCallbackEvent.clientId.getUtf8Bytes,
      "userId" -> pullCallbackEvent.userId.getUtf8Bytes,
      "eventType" -> pullCallbackEvent.eventType.getUtf8Bytes,
      "appName" -> pullCallbackEvent.appName.getUtf8Bytes,
      "contextId" -> pullCallbackEvent.contextId.getUtf8Bytes,
      "cargo" -> Option(pullCallbackEvent.cargo).map(_.getUtf8Bytes).orNull,
      "timestamp" -> pullCallbackEvent.timestamp.getBytes
    )
  }

  override def mapToChannelEvent(channelEventPropsMap: Map[String, Array[Byte]]): CallbackEvent = {
    PullCallbackEvent(
      messageId = channelEventPropsMap.getS("messageId"),
      eventId = channelEventPropsMap.getS("eventId"),
      userId = channelEventPropsMap.getS("userId"),
      eventType = channelEventPropsMap.getS("eventType"),
      appName = channelEventPropsMap.getS("appName"),
      contextId = channelEventPropsMap.getS("contextId"),
      clientId = channelEventPropsMap.getS("clientId"),
      cargo = channelEventPropsMap.getS("cargo"),
      timestamp = channelEventPropsMap.getL("timestamp").asInstanceOf[Long]
    )
  }

  override def fetchCallbackEvents(requestId: String, event: ChannelRequestInfo, fetchRange: Option[(Long, Long)]): Map[String, List[PullCallbackEvent]] = {
    val pullEvent = event.asInstanceOf[PullRequestInfo]
    pullEvent.userIds.toList.map(pullEvent.appName + _).flatMap(fetchCallbackEvents(requestId, _, fetchRange)).asInstanceOf[List[(PullCallbackEvent, Long)]].map(_._1).groupBy(_.userId)
  }

  override def fetchEventMapFromList(event: List[CallbackEvent]): Map[String, List[PullCallbackEvent]] = {
    val pullEvents = event.map(_.asInstanceOf[PullCallbackEvent])
    pullEvents.groupBy(p => p.messageId)
  }
}

object PullCallbackDao {
  def apply(tableName: String, hTableFactory: THTableFactory) =
    new PullCallbackDao(tableName: String, hTableFactory: THTableFactory)
}
