package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.THTableFactory
import com.flipkart.connekt.commons.iomodels._

class PullRequestDao(tableName: String, hTableFactory: THTableFactory) extends RequestDao(tableName: String, hTableFactory: THTableFactory) {

  override protected def channelRequestInfoMap(channelRequestInfo: ChannelRequestInfo): Map[String, Array[Byte]] = {
    val pullInfo = channelRequestInfo.asInstanceOf[PullRequestInfo]

    val m = scala.collection.mutable.Map[String, Array[Byte]]()

    pullInfo.topic.foreach(m += "topic" -> _.toString.getUtf8Bytes)
    Option(pullInfo.userIds).foreach(m += "userId" -> _.mkString(",").getUtf8Bytes)
    Option(pullInfo.platform).foreach(m += "platform" -> _.toString.getUtf8Bytes)
    Option(pullInfo.appName).foreach(m += "appName" -> _.toString.getUtf8Bytes)
    Option(pullInfo.ackRequired).foreach(m += "ackRequired" -> _.getBytes)
    Option(pullInfo.delayWhileIdle).foreach(m += "delayWhileIdle" -> _.getBytes)

    m.toMap
  }

  override protected def getChannelRequestInfo(reqInfoProps: Map[String, Array[Byte]]): ChannelRequestInfo = {
    PullRequestInfo(platform = reqInfoProps.getS("platform"),
      appName = reqInfoProps.getS("appName"),
      userIds = reqInfoProps.getS("userId").split(",").toSet,
      topic = Option(reqInfoProps.getS("topic")),
      ackRequired = reqInfoProps.getB("ackRequired"),
      delayWhileIdle = reqInfoProps.getB("delayWhileIdle")
    )
  }

  override protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]] = {
    Option(channelRequestData).map(d => {
      val pullRequestData = d.asInstanceOf[PullRequestData]
      Option(pullRequestData.data).map(m => Map[String, Array[Byte]]("data" -> m.toString.getUtf8Bytes)).orNull
    }).orNull
  }

  override protected def getChannelRequestData(reqDataProps: Map[String, Array[Byte]]): ChannelRequestData = {
    Option(reqDataProps.getKV("data")).map(PullRequestData).orNull
  }
}

object PullRequestDao {
  def apply(tableName: String = "fk-connekt-pull-info", hTableFactory: THTableFactory) =
    new PullRequestDao(tableName, hTableFactory)
}
