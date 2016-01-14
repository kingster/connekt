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
}

object PNRequestDao {
  def apply(tableName: String = "fk-connekt-pn-info", pullRequestTableName: String = "fk-connekt-pull-info", hTableFactory: HTableFactory) =
    new PNRequestDao(tableName, pullRequestTableName, hTableFactory)
}