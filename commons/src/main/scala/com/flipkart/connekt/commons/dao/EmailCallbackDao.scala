package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.iomodels.CallbackEvent

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
class EmailCallbackDao(tableName: String, hTableFactory: HTableFactory) extends CallbackDao(tableName: String, hTableFactory: HTableFactory) {
  override def channelEventPropsMap(channelCallbackEvent: CallbackEvent): Map[String, Array[Byte]] = ???

  override def mapToChannelEvent(channelEventPropsMap: Map[String, Array[Byte]]): CallbackEvent = ???
}
