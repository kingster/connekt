/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package fkint.mp.connekt

import com.flipkart.connekt.commons.entities.bigfoot.EventBaseSchema

case class PNCallbackEvent(messageId: String, appName: String, contextId: String, eventType: String, cargo: String, deviceId: String, platform: String, timestamp: String) extends EventBaseSchema {
  override def validate(): Unit = {}

  override def getSchemaVersion: String = "1.0"
}
