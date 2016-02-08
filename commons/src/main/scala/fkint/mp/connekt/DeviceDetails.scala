package fkint.mp.connekt

import com.flipkart.seraph.schema.BaseSchema
import org.joda.time.format.DateTimeFormatter

/**
 * Created by nidhi.mehla on 02/02/16.
 */
case class DeviceDetails(deviceId: String, userId: String, token: String, osName: String, osVersion: String,
                         appName: String, appVersion: String, brand: String, model: String, state: String = "",
                         ts: String, active: Boolean = true) extends BaseSchema {
  override def getSchemaVersion: String = "1.0"

  override def validate(): Unit = {}
}
