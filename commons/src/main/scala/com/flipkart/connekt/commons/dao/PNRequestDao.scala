package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._

import scala.collection.mutable.ListBuffer


/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
class PNRequestDao(tableName: String, pullRequestTableName: String, hTableFactory: HTableFactory) extends RequestDao(tableName: String, hTableFactory: HTableFactory) {
  override protected def channelRequestInfoMap(channelRequestInfo: ChannelRequestInfo): Map[String, Array[Byte]] = {
    val pnRequestInfo = channelRequestInfo.asInstanceOf[PNRequestInfo]

    Map[String, Array[Byte]](
      "platform" -> pnRequestInfo.platform.toString.getUtf8Bytes,
      "appName" -> pnRequestInfo.appName.getUtf8Bytes,
      "deviceId" -> pnRequestInfo.deviceId.getUtf8Bytes,
      "ackRequired" -> pnRequestInfo.ackRequired.getBytes,
      "delayWhileIdle" -> pnRequestInfo.delayWhileIdle.getBytes
    )
  }

  override protected def getChannelRequestInfo(reqInfoProps: Map[String, Array[Byte]]): ChannelRequestInfo = {
    PNRequestInfo(
      platform = reqInfoProps.getS("platform"),
      appName = reqInfoProps.getS("appName"),
      deviceId = reqInfoProps.getS("deviceId"),
      ackRequired = reqInfoProps.getB("ackRequired"),
      delayWhileIdle = reqInfoProps.getB("delayWhileIdle")
    )
  }

  override protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]] = {
    val pnRequestData = channelRequestData.asInstanceOf[PNRequestData]

    Map[String, Array[Byte]](
      "data" -> pnRequestData.data.toString.getUtf8Bytes
    )
  }

  override protected def getChannelRequestData(reqDataProps: Map[String, Array[Byte]]): ChannelRequestData = {
    PNRequestData(data = reqDataProps.getKV("data"))
  }

  override def pullRequestMetaMap(requestId: String, channelRequestInfo: ChannelRequestInfo): (String, Map[String, Array[Byte]]) = {
    val pnRequestInfo = channelRequestInfo.asInstanceOf[PNRequestInfo]

    (pullRequestRowKey(pnRequestInfo.deviceId, requestId), Map[String, Array[Byte]]("isRead" -> false.getBytes))
  }

  override protected def getPullRequestIds(subscriberId: String, minTimestamp: Long, maxTimestamp: Long): List[String] = {
    implicit val hTableInterface = hTableFactory.getTableInterface(pullRequestTableName)
    try {
      val colFamiliesReqd = List("m")
      val rawData = fetchRows(pullRequestTableName, subscriberId, s"${subscriberId}{", colFamiliesReqd, Some(minTimestamp, maxTimestamp))
      val requestIds = ListBuffer[String]()

      rawData.foreach(tuple => {
        val requestId = tuple._1.split("-").last
        tuple._2.get("m").foreach(_.get("isRead").foreach(w => if(!w.getBoolean) requestIds += requestId))
      })

      requestIds.toList
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching pull request ids failed for $subscriberId, ${e.getMessage}", e)
        throw new IOException(s"Fetching pull request ids failed for $subscriberId", e)
    } finally {
      hTableFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def pullRequestMetaTable: String = pullRequestTableName

  private def pullRequestRowKey(subscriptionId: String, requestId: String) = s"${com.flipkart.connekt.commons.utils.StringUtils.md5(subscriptionId)}-$requestId"

  def fetchPNRequestInfo(id: String): Option[PNRequestInfo] = {
    fetchRequestInfo(id).map(_.asInstanceOf[PNRequestInfo])
  }

  override protected def savePullRequestIds(requestId: String, channelRequestInfo: ChannelRequestInfo): Unit = {
    implicit val hTableInterface = hTableFactory.getTableInterface(pullRequestTableName)
    try {
      val pnRequestInfo = channelRequestInfo.asInstanceOf[PNRequestInfo]

      val rawRequestData = Map[String, Map[String, Array[Byte]]]("m" -> Map[String, Array[Byte]]("isRead" -> false.getBytes))
      addRow(pullRequestTableName, pullRequestRowKey(pnRequestInfo.deviceId, requestId), rawRequestData)

      ConnektLogger(LogFile.DAO).info(s"Pull requestId mapping persisted for $requestId")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Pull requestId mapping persistence failed for $requestId ${e.getMessage}", e)
        throw new IOException(s"Pull requestId mapping persistence failed for $requestId", e)
    } finally {
      hTableFactory.releaseTableInterface(hTableInterface)
    }
  }
}

object PNRequestDao {
  def apply(tableName: String = "fk-connekt-pn-info", pullRequestTableName: String = "fk-connekt-pull-info", hTableFactory: HTableFactory) =
    new PNRequestDao(tableName, pullRequestTableName, hTableFactory)
}