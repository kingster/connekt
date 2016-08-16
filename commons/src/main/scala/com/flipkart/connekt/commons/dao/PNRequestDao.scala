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

import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.THTableFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils

class PNRequestDao(tableName: String, pullRequestTableName: String, hTableFactory: THTableFactory) extends RequestDao(tableName: String, hTableFactory: THTableFactory) {

  override protected def channelRequestInfoMap(channelRequestInfo: ChannelRequestInfo): Map[String, Array[Byte]] = {
    val pnRequestInfo = channelRequestInfo.asInstanceOf[PNRequestInfo]

    val m = scala.collection.mutable.Map[String, Array[Byte]]()

    pnRequestInfo.topic.foreach(m += "topic" -> _.toString.getUtf8Bytes)
    Option(pnRequestInfo.deviceIds).foreach(m += "deviceId" -> _.mkString(",").getUtf8Bytes)
    Option(pnRequestInfo.platform).foreach(m += "platform" -> _.toString.getUtf8Bytes)
    Option(pnRequestInfo.appName).foreach(m += "appName" -> _.toString.getUtf8Bytes)
    Option(pnRequestInfo.ackRequired).foreach(m += "ackRequired" -> _.getBytes)
    Option(pnRequestInfo.delayWhileIdle).foreach(m += "delayWhileIdle" -> _.getBytes)

    m.toMap
  }

  override protected def getChannelRequestInfo(reqInfoProps: Map[String, Array[Byte]]): ChannelRequestInfo = PNRequestInfo(
    platform = reqInfoProps.getS("platform"),
    appName = reqInfoProps.getS("appName"),
    deviceIds = reqInfoProps.getS("deviceId").split(",").toSet,
    topic = Option(reqInfoProps.getS("topic")),
    ackRequired = reqInfoProps.getB("ackRequired"),
    delayWhileIdle = reqInfoProps.getB("delayWhileIdle")
  )

  override protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]] = {
    Option(channelRequestData).map(d => {
      val pnRequestData = d.asInstanceOf[PNRequestData]
      Option(pnRequestData.data).map(m => "data" -> m.toString.getUtf8Bytes).toMap ++ Option(pnRequestData.pushType).map(m => "pushType" -> m.getUtf8Bytes).toMap
    }).orNull
  }

  override protected def getChannelRequestData(reqDataProps: Map[String, Array[Byte]]): ChannelRequestData = {
    val data = reqDataProps.getKV("data")
    val pushType = reqDataProps.getS("pushType")

    if(StringUtils.isNullOrEmpty(data) && StringUtils.isNullOrEmpty(pushType) )
      null
    else
      PNRequestData(pushType = pushType, data = data)
  }

  def fetchPNRequestInfo(id: String): Option[PNRequestInfo] = {
    fetchRequestInfo(id).map(_.asInstanceOf[PNRequestInfo])
  }
}

object PNRequestDao {
  def apply(tableName: String = "fk-connekt-pn-info", pullRequestTableName: String = "fk-connekt-pull-info", hTableFactory: THTableFactory) =
    new PNRequestDao(tableName, pullRequestTableName, hTableFactory)
}
