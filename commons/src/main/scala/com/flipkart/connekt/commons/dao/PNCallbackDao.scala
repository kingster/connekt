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

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.iomodels.{CallbackEvent, ChannelRequestInfo, PNCallbackEvent, PNRequestInfo}

class PNCallbackDao(tableName: String, hTableFactory: HTableFactory) extends CallbackDao(tableName: String, hTableFactory: HTableFactory) {

  override def channelEventPropsMap(channelCallbackEvent: CallbackEvent): Map[String, Array[Byte]] = {
    val pnCallbackEvent = channelCallbackEvent.asInstanceOf[PNCallbackEvent]

    Map[String, Array[Byte]](
      "messageId" -> pnCallbackEvent.messageId.getUtf8Bytes,
      "deviceId" -> pnCallbackEvent.deviceId.getUtf8Bytes,
      "eventType" -> pnCallbackEvent.eventType.getUtf8Bytes,
      "platform" -> pnCallbackEvent.platform.getUtf8Bytes,
      "appName" -> pnCallbackEvent.appName.getUtf8Bytes,
      "contextId" -> pnCallbackEvent.contextId.getUtf8Bytes,
      "cargo" -> Option(pnCallbackEvent.cargo).map(_.getUtf8Bytes).orNull,
      "timestamp" -> pnCallbackEvent.timestamp.getBytes
    )
  }

  override def mapToChannelEvent(channelEventPropsMap: Map[String, Array[Byte]]): CallbackEvent = {
    PNCallbackEvent(
      messageId = channelEventPropsMap.getS("messageId"),
      deviceId = channelEventPropsMap.getS("deviceId"),
      eventType = channelEventPropsMap.getS("eventType"),
      platform = channelEventPropsMap.getS("platform"),
      appName = channelEventPropsMap.getS("appName"),
      contextId = channelEventPropsMap.getS("contextId"),
      cargo = channelEventPropsMap.getS("cargo"),
      timestamp = channelEventPropsMap.getL("timestamp").asInstanceOf[Long]
    )
  }

  override def fetchCallbackEvents(requestId: String, event: ChannelRequestInfo, fetchRange: Option[(Long, Long)]): Map[String, List[PNCallbackEvent]] = {
    val pnEvent = event.asInstanceOf[PNRequestInfo]
    pnEvent.deviceId.map(pnEvent.appName + _).flatMap(fetchCallbackEvents(requestId, _, fetchRange)).asInstanceOf[List[PNCallbackEvent]].groupBy(_.deviceId)
  }

  override def fetchEventMapFromList(event: List[CallbackEvent]): Map[String, List[PNCallbackEvent]] = {
    val pnEvents = event.map(_.asInstanceOf[PNCallbackEvent])
    pnEvents.groupBy(p => p.messageId)
  }
}

object PNCallbackDao {
  def apply(tableName: String, hTableFactory: HTableFactory) =
    new PNCallbackDao(tableName: String, hTableFactory: HTableFactory)
}
