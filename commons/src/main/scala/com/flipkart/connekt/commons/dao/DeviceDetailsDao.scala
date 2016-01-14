package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.roundeights.hasher.Implicits._


/**
 *
 *
 * @author durga.s
 * @version 11/23/15
 */
class DeviceDetailsDao(tableName: String, hTableFactory: HTableFactory) extends Dao with HbaseDao {
  val hTableConnFactory = hTableFactory

  val hTableName = tableName
  val hUserIndexTableName = tableName + "-user-index"
  val hTokenIndexTableName = tableName + "-token-index"


  private def getRowKey(appName: String, deviceId: String) = appName.toLowerCase + "_" + deviceId

  private def getUserIndexRowKey(appName :String, deviceId:String, userId:String) = appName.toLowerCase + "_"  + userId + "_" + deviceId
  private def getUserIndexRowPrefix(appName :String,  userId:String) = appName.toLowerCase + "_"  + userId + "_"

  private def getTokenIndexRowKey(appName :String, deviceId:String, tokenId:String) = appName.toLowerCase + "_"  + tokenId.sha256.hash.hex + "_" + deviceId
  private def getTokenIndexRowPrefix(appName :String,  tokenId:String) = appName.toLowerCase + "_"  + tokenId.sha256.hash.hex + "_"

  def add(deviceDetails: DeviceDetails)  = {

    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    val hUserIndexTableInterface = hTableConnFactory.getTableInterface(hUserIndexTableName)
    val hTokenIndexTableInterface = hTableConnFactory.getTableInterface(hTokenIndexTableName)

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

      // Add secondary indexes.
      addRow(hUserIndexTableName, getUserIndexRowKey(deviceDetails.appName, deviceDetails.deviceId,deviceDetails.userId), HbaseDao.emptyRowData)(hUserIndexTableInterface)
      addRow(hTokenIndexTableName, getTokenIndexRowKey(deviceDetails.appName, deviceDetails.deviceId, deviceDetails.token), HbaseDao.emptyRowData)(hTokenIndexTableInterface)


      ConnektLogger(LogFile.DAO).info(s"DeviceDetails registered for ${deviceDetails.deviceId}")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).info(s"DeviceDetails registration failed for ${deviceDetails.deviceId}, ${e.getMessage}")
        throw new IOException("DeviceDetails registration failed for %s".format(deviceDetails.deviceId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
      hTableConnFactory.releaseTableInterface(hUserIndexTableInterface)
      hTableConnFactory.releaseTableInterface(hTokenIndexTableInterface)
    }
  }

  def get(appName: String, deviceId: String): Option[DeviceDetails] = {
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

  def getByTokenId(appName: String, tokenId: String): Option[DeviceDetails] = {

    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTokenIndexTableName)
    val rowKeyPrefix = getTokenIndexRowPrefix(appName, tokenId)
    val deviceIndex = fetchRowKeys(hTokenIndexTableName,rowKeyPrefix, rowKeyPrefix+"{",List("d"))

    deviceIndex.headOption.map(_.split("_").last).flatMap(get(appName, _))
  }

  def getByUserId(appName: String, accId: String): List[DeviceDetails] = {

    implicit val hTableInterface = hTableConnFactory.getTableInterface(hUserIndexTableName)
    val rowKeyPrefix = getUserIndexRowPrefix(appName, accId)
    val devices = fetchRowKeys(hUserIndexTableName,rowKeyPrefix, rowKeyPrefix+"{",List("d"))
    devices.map(_.split("_").last).flatMap(get(appName, _))
  }
}

object DeviceDetailsDao {
  def apply(tableName: String, hTableFactory: HTableFactory) = new DeviceDetailsDao(tableName, hTableFactory)
}