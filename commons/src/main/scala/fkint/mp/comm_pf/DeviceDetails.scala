package fkint.mp.comm_pf

import com.flipkart.seraph.schema.BaseSchema

/**
 * Created by nidhi.mehla on 02/02/16.
 */
case class DeviceDetails(deviceId: String, userId: String, token: String, osName: String, osVersion: String,
                         appName: String, appVersion: String, brand: String, model: String, state: String = "", ts: Long, active: Boolean = true) extends BaseSchema {
  override def getSchemaVersion: String = "3.0"

  override def validate(): Unit = {}
}
