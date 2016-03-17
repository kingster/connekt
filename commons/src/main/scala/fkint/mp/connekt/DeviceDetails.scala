/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package fkint.mp.connekt

import com.flipkart.connekt.commons.entities.bigfoot.EntityBaseSchema

case class DeviceDetails(deviceId: String, userId: String, token: String, osName: String, osVersion: String,
                         appName: String, appVersion: String, brand: String, model: String, state: String = "",
                         ts: String, active: Boolean = true) extends EntityBaseSchema {
  override def getSchemaVersion: String = "1.0"

  override def validate(): Unit = {}

  override def id: String = deviceId
}
