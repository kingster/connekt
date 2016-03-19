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

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.iomodels._

class PNRequestDao(tableName: String, pullRequestTableName: String, hTableFactory: HTableFactory) extends RequestDao(tableName: String, hTableFactory: HTableFactory) {

  override protected def channelRequestInfoMap(channelRequestInfo: ChannelRequestInfo): Map[String, Array[Byte]] = {
    val pnRequestInfo = channelRequestInfo.asInstanceOf[PNRequestInfo]

    val m = scala.collection.mutable.Map[String, Array[Byte]]()

    Option(pnRequestInfo.deviceId).foreach(m += "deviceId" -> _.mkString(",").getUtf8Bytes)
    Option(pnRequestInfo.platform).foreach(m += "platform" -> _.toString.getUtf8Bytes)
    Option(pnRequestInfo.appName).foreach(m += "appName" -> _.toString.getUtf8Bytes)
    Option(pnRequestInfo.ackRequired).foreach(m += "ackRequired" -> _.getBytes)
    Option(pnRequestInfo.delayWhileIdle).foreach(m += "delayWhileIdle" -> _.getBytes)

    m.toMap
  }

  override protected def getChannelRequestInfo(reqInfoProps: Map[String, Array[Byte]]): ChannelRequestInfo = {
    PNRequestInfo(
      platform = reqInfoProps.getS("platform"),
      appName = reqInfoProps.getS("appName"),
      deviceId = reqInfoProps.getS("deviceId").split(",").toList,
      ackRequired = reqInfoProps.getB("ackRequired"),
      delayWhileIdle = reqInfoProps.getB("delayWhileIdle")
    )
  }

  override protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]] = {
    Option(channelRequestData).map(d => {
      val pnRequestData = d.asInstanceOf[PNRequestData]
      Option(pnRequestData.data).map(m => Map[String, Array[Byte]]("data" -> m.toString.getUtf8Bytes)).orNull
    }).orNull
  }

  override protected def getChannelRequestData(reqDataProps: Map[String, Array[Byte]]): ChannelRequestData = {
    Option(reqDataProps.getKV("data")).map(PNRequestData).orNull
  }

  def fetchPNRequestInfo(id: String): Option[PNRequestInfo] = {
    fetchRequestInfo(id).map(_.asInstanceOf[PNRequestInfo])
  }
}

object PNRequestDao {
  def apply(tableName: String = "fk-connekt-pn-info", pullRequestTableName: String = "fk-connekt-pull-info", hTableFactory: HTableFactory) =
    new PNRequestDao(tableName, pullRequestTableName, hTableFactory)
}
