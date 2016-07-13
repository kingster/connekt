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

import java.io.IOException

import com.flipkart.connekt.commons.dao.HbaseDao.RowData
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, THTableFactory}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.metrics.Timed
import com.roundeights.hasher.Implicits._
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.{BinaryComparator, CompareFilter, FilterList, SingleColumnValueFilter}
import org.apache.hadoop.hbase.util.Bytes

import scala.collection.JavaConversions._
import scala.collection.mutable

class DeviceDetailsDao(tableName: String, hTableFactory: THTableFactory) extends Dao with HbaseDao with Instrumented {
  val hTableConnFactory = hTableFactory

  val hTableName = tableName
  val hUserIndexTableName = tableName + "-user-index"
  val hTokenIndexTableName = tableName + "-token-index"

  val dataColFamilies = List("p", "a")

  private def getRowKey(appName: String, deviceId: String) = deviceId.sha256.hash.hex + "_" + appName.toLowerCase

  private def getUserIndexRowKey(appName: String, deviceId: String, userId: String) = userId.sha256.hash.hex + "_" + appName.toLowerCase + "_" + deviceId

  private def getUserIndexRowPrefix(appName: String, userId: String) = userId.sha256.hash.hex + "_" + appName.toLowerCase + "_"

  private def getTokenIndexRowKey(appName: String, deviceId: String, tokenId: String) = tokenId.sha256.hash.hex + "_" + appName.toLowerCase + "_" + deviceId

  private def getTokenIndexRowPrefix(appName: String, tokenId: String) = tokenId.sha256.hash.hex + "_" + appName.toLowerCase + "_"

  @Timed("add")
  def add(appName: String, deviceDetails: DeviceDetails) = {

    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    val hUserIndexTableInterface = hTableConnFactory.getTableInterface(hUserIndexTableName)
    val hTokenIndexTableInterface = hTableConnFactory.getTableInterface(hTokenIndexTableName)

    try {
      val deviceRegInfoCfProps = mutable.Map[String, Array[Byte]](
        "deviceId" -> deviceDetails.deviceId.getUtf8Bytes,
        "userId" -> deviceDetails.userId.getUtf8BytesNullWrapped,
        "token" -> deviceDetails.token.getUtf8Bytes,
        "osName" -> deviceDetails.osName.getUtf8Bytes,
        "osVersion" -> deviceDetails.osVersion.getUtf8BytesNullWrapped,
        "appName" -> deviceDetails.appName.getUtf8Bytes,
        "appVersion" -> deviceDetails.appVersion.getUtf8Bytes
      )

      if(deviceDetails.keys != null && deviceDetails.keys.nonEmpty)
        deviceRegInfoCfProps += "keys" -> deviceDetails.keys.getJson.getUtf8Bytes

      val deviceMetaCfProps = Map[String, Array[Byte]](
        "brand" -> deviceDetails.brand.getUtf8BytesNullWrapped,
        "model" -> deviceDetails.model.getUtf8BytesNullWrapped,
        "state" -> deviceDetails.state.getUtf8BytesNullWrapped
      )

      val rawData = Map[String, Map[String, Array[Byte]]]("p" -> deviceRegInfoCfProps.toMap, "a" -> deviceMetaCfProps)
      addRow(getRowKey(deviceDetails.appName, deviceDetails.deviceId), rawData)

      // Add secondary indexes.
      addRow(getTokenIndexRowKey(deviceDetails.appName, deviceDetails.deviceId, deviceDetails.token), HbaseDao.emptyRowData)(hTokenIndexTableInterface)

      if (!StringUtils.isNullOrEmpty(deviceDetails.userId))
        addRow(getUserIndexRowKey(deviceDetails.appName, deviceDetails.deviceId, deviceDetails.userId), HbaseDao.emptyRowData)(hUserIndexTableInterface)


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

  @Timed("get")
  def get(appName: String, deviceId: String): Option[DeviceDetails] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val rawData = fetchRow(getRowKey(appName, deviceId), dataColFamilies)
      extractDeviceDetails(rawData)
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching DeviceDetails failed for $deviceId, ${e.getMessage}")
        throw new IOException("Fetching DeviceDetails failed for %s".format(deviceId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  @Timed("mget")
  def get(appName: String, deviceIds: Set[String]): List[DeviceDetails] = {
    if(deviceIds.nonEmpty) {
      implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
      try {
        fetchMultiRows(deviceIds.toList.map(getRowKey(appName, _)), dataColFamilies).values.flatMap(extractDeviceDetails).toList
      } catch {
        case e: IOException =>
          ConnektLogger(LogFile.DAO).error(s"Fetching DeviceDetails failed for $deviceIds, ${e.getMessage}")
          throw new IOException("Fetching DeviceDetails failed for %s".format(deviceIds), e)
      } finally {
        hTableConnFactory.releaseTableInterface(hTableInterface)
      }
    } else Nil
  }

  @Timed("getAll")
  def getAll(appName: String): Stream[DeviceDetails] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val scan = new Scan()
      val filters = new FilterList()

      val appNameFilter = new SingleColumnValueFilter("p".getBytes, "appName".getBytes,
        CompareFilter.CompareOp.EQUAL, new BinaryComparator(appName.getBytes))
      filters.addFilter(appNameFilter)
      scan.setFilter(filters)

      val resultScanner = hTableInterface.getScanner(scan)

      resultScanner.iterator().toStream.flatMap( rI => {
        val resultMap: RowData = getRowData(rI, dataColFamilies)
        extractDeviceDetails(resultMap)
      })

    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Error Fetching All DeviceDetails failed for $appName, ${e.getMessage}")
        throw e;
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)

    }
  }

  @Timed("getByTokenId")
  def getByTokenId(appName: String, tokenId: String): Option[DeviceDetails] = {

    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTokenIndexTableName)
    val rowKeyPrefix = getTokenIndexRowPrefix(appName, tokenId)
    val deviceIndex = fetchRowKeys(rowKeyPrefix, rowKeyPrefix + "{", List("d"))
    hTableConnFactory.releaseTableInterface(hTableInterface)

    deviceIndex.headOption.map(_.split("_").last).flatMap(get(appName, _))
  }

  @Timed("getByUserId")
  def getByUserId(appName: String, accId: String): List[DeviceDetails] = {

    implicit val hTableInterface = hTableConnFactory.getTableInterface(hUserIndexTableName)
    val rowKeyPrefix = getUserIndexRowPrefix(appName, accId)
    val devices = fetchRowKeys(rowKeyPrefix, rowKeyPrefix + "{", List("d"))
    hTableConnFactory.releaseTableInterface(hTableInterface)

    devices.map(_.split("_").last).flatMap(get(appName, _))
  }

  /**
   * Update takes care of updateing/removeing older index's and then updating the deviceDetails
   * @param appName
   * @param deviceId
   * @param deviceDetails
   */
  @Timed("update")
  def update(appName: String, deviceId: String, deviceDetails: DeviceDetails) = {
    val current = get(appName, deviceId)
    val update = deviceDetails.copy(deviceId = deviceId) //override, to take care of developer mistakes
    current.foreach(existingDetails => {
      if (existingDetails.token != update.token)
        deleteTokenIdIndex(appName, deviceId, existingDetails.token)
      if (!StringUtils.isNullOrEmpty(existingDetails.userId) && existingDetails.userId != update.userId)
        deleteUserIdIndex(appName, deviceId, existingDetails.userId)
      add(appName, update)
    })
  }

  @Timed("delete")
  def delete(appName: String, deviceId: String) = {
    val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    val hUserIndexTableInterface = hTableConnFactory.getTableInterface(hUserIndexTableName)
    val hTokenIndexTableInterface = hTableConnFactory.getTableInterface(hTokenIndexTableName)

    try {
      get(appName, deviceId) match {
        case Some(device) =>
          removeRow(getRowKey(appName, deviceId))(hTableInterface)
          removeRow(getTokenIndexRowKey(appName, deviceId, device.token))(hTokenIndexTableInterface)
          StringUtils.isNullOrEmpty(device.userId) match {
            case false =>
              removeRow(getUserIndexRowKey(appName, deviceId, device.userId))(hUserIndexTableInterface)
            case true =>
          }
        case None =>
      }
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
      hTableConnFactory.releaseTableInterface(hUserIndexTableInterface)
      hTableConnFactory.releaseTableInterface(hTokenIndexTableInterface)
    }
  }

  private def deleteTokenIdIndex(appName: String, deviceId: String, tokenId: String) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTokenIndexTableName)
    val rowKey = getTokenIndexRowKey(appName, deviceId, tokenId)
    removeRow(rowKey)
    hTableConnFactory.releaseTableInterface(hTableInterface)
  }

  private def deleteUserIdIndex(appName: String, deviceId: String, userId: String) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hUserIndexTableName)
    val rowKey = getUserIndexRowKey(appName, deviceId, userId)
    removeRow(rowKey)
    hTableConnFactory.releaseTableInterface(hTableInterface)
  }

  private def extractDeviceDetails(data: RowData): Option[DeviceDetails] = {
    val devRegProps = data.get("p")
    val devMetaProps = data.get("a")
    val allProps = devRegProps.flatMap[Map[String, Array[Byte]]](r => devMetaProps.map[Map[String, Array[Byte]]](m => m ++ r))
    allProps.map(fields => {

      def getOption(key:String)=  fields.get(key).map(v => v.getString)
      def get(key: String) = getOption(key).orNull
      def getNullableString(key: String) = fields.get(key).map(v => v.getStringNullable).orNull

      DeviceDetails(
        deviceId = get("deviceId"),
        userId = getNullableString("userId"),
        token = get("token"),
        osName = get("osName"),
        osVersion = getNullableString("osVersion"),
        appName = get("appName"),
        appVersion = get("appVersion"),
        brand = getNullableString("brand"),
        model = getNullableString("model"),
        state = getNullableString("state"),
        keys = getOption("keys").map(_.getObj[Map[String,String]]).getOrElse(Map.empty[String,String])
      )
    })
  }


}

object DeviceDetailsDao {
  def apply(tableName: String, hTableFactory: THTableFactory) = new DeviceDetailsDao(tableName, hTableFactory)
}
