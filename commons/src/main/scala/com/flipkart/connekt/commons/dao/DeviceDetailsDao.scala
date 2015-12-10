package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.utils.StringUtils._


/**
 *
 *
 * @author durga.s
 * @version 11/23/15
 */
class DeviceDetailsDao(tableName: String, hTableFactory: HTableFactory) extends Dao with HbaseDao {
  val hTableConnFactory = hTableFactory
  val hTableName = tableName

  private def getRowKey(appName: String, deviceId: String) = appName.toLowerCase + "_" + deviceId

  def saveDeviceDetails(deviceDetails: DeviceDetails)  = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val deviceRegInfoCfProps = Map[String, Array[Byte]](
        "deviceId" -> deviceDetails.deviceId.getUtf8Bytes,
        "userId" -> deviceDetails.userId.getUtf8Bytes,
        "token" -> deviceDetails.token.getUtf8Bytes,
        "osName" -> deviceDetails.osName.getUtf8Bytes,
        "osVersion" -> deviceDetails.osVersion.getUtf8Bytes,
        "appName" -> deviceDetails.appName.getUtf8Bytes,
        "appVersion" -> deviceDetails.appVersion.getUtf8Bytes
      )

      val deviceMetaCfProps = Map[String, Array[Byte]](
        "brand" -> deviceDetails.brand.getUtf8Bytes,
        "model" -> deviceDetails.model.getUtf8Bytes,
        "state" -> deviceDetails.state.getUtf8Bytes,
        "altPush" -> deviceDetails.altPush.toString.getUtf8Bytes
      )

      val rawData = Map[String, Map[String, Array[Byte]]]("p" -> deviceRegInfoCfProps, "a" -> deviceMetaCfProps)
      addRow(hTableName, getRowKey(deviceDetails.appName, deviceDetails.deviceId), rawData)

      ConnektLogger(LogFile.DAO).info(s"DeviceDetails registered for ${deviceDetails.deviceId}")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).info(s"DeviceDetails registration failed for ${deviceDetails.deviceId}, ${e.getMessage}")
        throw new IOException("DeviceDetails registration failed for %s".format(deviceDetails.deviceId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  def fetchDeviceDetails(appName: String, deviceId: String): Option[DeviceDetails] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val colFamiliesReqd = List("p", "a")
      val rawData = fetchRow(hTableName, getRowKey(appName, deviceId) , colFamiliesReqd)

      val devRegProps = rawData.get("p")
      val devMetaProps = rawData.get("a")

      val allProps = devRegProps.flatMap[Map[String, Array[Byte]]](r => devMetaProps.map[Map[String, Array[Byte]]](m => m ++ r))

      allProps.map(fields => {
        def get(key: String) = fields.get(key).map(new String(_)).orNull
        def getB(key: String) = fields.get(key).exists(b => java.lang.Boolean.parseBoolean(new String(b)))

        DeviceDetails(
          deviceId = get("deviceId"),
          userId = get("userId"),
          token = get("token"),
          osName = get("osName"),
          osVersion = get("osVersion"),
          appName = get("appName"),
          appVersion = get("appVersion"),
          brand = get("brand"),
          model = get("model"),
          state = get("state"),
          altPush = getB("altPush")
        )
      })

    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching DeviceDetails failed for $deviceId, ${e.getMessage}")
        throw new IOException("Fetching DeviceDetails failed for %s".format(deviceId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }
}

object DeviceDetailsDao {

  def apply(tableName: String = "connekt-device-info", hTableFactory: HTableFactory) =
    new DeviceDetailsDao(tableName, hTableFactory)
}