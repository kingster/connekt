package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, PNRequestData}


/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
class PNRequestDao(tableName: String, hTableFactory: HTableFactory) extends RequestDao(tableName: String, hTableFactory: HTableFactory) {

  override protected def channelRequestDataMap(pnChannelData: ChannelRequestData): Map[String, Array[Byte]] = {
    val pnRequestData = pnChannelData.asInstanceOf[PNRequestData]

    Map[String, Array[Byte]](
      "appName" -> pnRequestData.appName.getUtf8Bytes,
      "deviceId" -> pnRequestData.deviceId.getUtf8Bytes,
      "ackRequired" -> pnRequestData.ackRequired.getBytes,
      "delayWhileIdle" -> pnRequestData.delayWhileIdle.getBytes,
      "data" -> pnRequestData.data.toString.getUtf8Bytes,
      "platform" -> pnRequestData.platform.toString.getUtf8Bytes
    )
  }

  override protected def getChannelRequestData(dataMap: Map[String, Array[Byte]]): ChannelRequestData = {
    PNRequestData(
      platform = dataMap.getS("platform"),
      appName = dataMap.getS("appName"),
      deviceId = dataMap.getS("deviceId"),
      ackRequired = dataMap.getB("ackRequired"),
      delayWhileIdle = dataMap.getB("delayWhileIdle"),
      data = dataMap.getS("data")
    )
  }
}

object PNRequestDao {
  def apply(tableName: String = "fk-connekt-pn-info", hTableFactory: HTableFactory) =
    new PNRequestDao(tableName, hTableFactory)
}