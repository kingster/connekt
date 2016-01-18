package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.iomodels.{ChannelRequestInfo, CallbackEvent}

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
class EmailCallbackDao(tableName: String, hTableFactory: HTableFactory) extends CallbackDao(tableName: String, hTableFactory: HTableFactory) {
  override def channelEventPropsMap(channelCallbackEvent: CallbackEvent): Map[String, Array[Byte]] = ???

  override def mapToChannelEvent(channelEventPropsMap: Map[String, Array[Byte]]): CallbackEvent = ???

  override def fetchEventMapFromList(event: List[CallbackEvent]): Map[String, List[CallbackEvent]] = ???

  override def fetchCallbackEvents(requestId: String, event: ChannelRequestInfo, fetchRange: Option[(Long, Long)]): Map[String, List[CallbackEvent]] = ???
}
