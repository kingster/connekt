package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.iomodels.{PNCallbackEvent, CallbackEvent}
import com.flipkart.connekt.commons.dao.HbaseDao._

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
class PNCallbackDao(tableName: String, hTableFactory: HTableFactory) extends CallbackDao(tableName: String, hTableFactory: HTableFactory) {

  override def channelEventPropsMap(channelCallbackEvent: CallbackEvent): Map[String, Array[Byte]] = {
    val pnCallbackEvent = channelCallbackEvent.asInstanceOf[PNCallbackEvent]

    Map[String, Array[Byte]](
      "requestId" -> pnCallbackEvent.messageId.getUtf8Bytes,
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
}

object PNCallbackDao {
  def apply(tableName: String, hTableFactory: HTableFactory) =
    new PNCallbackDao(tableName: String, hTableFactory: HTableFactory)
}
