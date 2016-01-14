package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ChannelRequestInfo, ConnektRequest}

import scala.collection.mutable.ListBuffer

/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
abstract class RequestDao(tableName: String, hTableFactory: HTableFactory) extends TRequestDao with HbaseDao {
  private val hTableConnFactory = hTableFactory
  private val hTableName = tableName

  protected def channelRequestInfoMap(channelRequestInfo: ChannelRequestInfo): Map[String, Array[Byte]]

  protected def getChannelRequestInfo(reqInfoProps: Map[String, Array[Byte]]): ChannelRequestInfo

  protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]]

  protected def getChannelRequestData(reqDataProps: Map[String, Array[Byte]]): ChannelRequestData

  protected def pullRequestMetaMap(requestId: String, channelRequestInfo: ChannelRequestInfo): (String, Map[String, Array[Byte]])

  protected def savePullRequestIds(requestId: String, channelRequestInfo: ChannelRequestInfo)

  protected def getPullRequestIds(subscriberId: String, minTimestamp: Long, maxTimestamp: Long): List[String]

  protected def pullRequestMetaTable: String

  override def saveRequest(requestId: String, request: ConnektRequest) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val requestProps = Map[String, Array[Byte]](
        "id" -> requestId.getUtf8Bytes,
        "channel" -> request.channel.getUtf8Bytes,
        "sla" -> request.sla.getUtf8Bytes,
        "templateId" -> request.templateId.getUtf8Bytes,
        "scheduleTs" -> request.scheduleTs.getBytes,
        "expiryTs" -> request.expiryTs.getBytes
      )

      val channelRequestInfoProps = channelRequestInfoMap(request.channelInfo)
      val channelRequestDataProps = channelRequestDataMap(request.channelData)

      val rawData = Map[String, Map[String, Array[Byte]]]("r" -> requestProps, "c" -> channelRequestInfoProps, "t" -> channelRequestDataProps)
      addRow(hTableName, requestId, rawData)

      ConnektLogger(LogFile.DAO).info(s"Request info persisted for $requestId")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Request info persistence failed for $requestId ${e.getMessage}", e)
        throw new IOException("Request info persistence failed for %s".format(requestId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def savePullRequest(requestId: String, request: ConnektRequest) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val requestProps = Map[String, Array[Byte]](
        "id" -> requestId.getUtf8Bytes,
        "channel" -> request.channel.getUtf8Bytes,
        "sla" -> request.sla.getUtf8Bytes,
        "templateId" -> request.templateId.getUtf8Bytes,
        "scheduleTs" -> request.scheduleTs.getBytes,
        "expiryTs" -> request.expiryTs.getBytes
      )

      val channelRequestInfoProps = channelRequestInfoMap(request.channelInfo)
      val channelRequestDataProps = channelRequestDataMap(request.channelData)

      val rawRequestData = Map[String, Map[String, Array[Byte]]]("r" -> requestProps, "c" -> channelRequestInfoProps, "t" -> channelRequestDataProps)
      addRow(hTableName, requestId, rawRequestData)

      savePullRequestIds(requestId, request.channelInfo)
      ConnektLogger(LogFile.DAO).info(s"Pull request info persisted for $requestId")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Pull request info persistence failed for $requestId ${e.getMessage}", e)
        throw new IOException("Pull request info persistence failed for %s".format(requestId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def fetchPullRequest(id: String, minTimestamp: Long, maxTimestamp: Long): List[ConnektRequest] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val requestIds = getPullRequestIds(id, minTimestamp, maxTimestamp)
      val requestInfoList = ListBuffer[ConnektRequest]()
      requestIds.foreach({
        fetchRequest(_).foreach(r => requestInfoList += r)
      })

      requestInfoList.toList
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching Pull request info failed for subscriber: $id, ${e.getMessage}", e)
        throw new IOException(s"Fetching Pull request info failed for subscriber $id", e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def fetchRequest(connektId: String): Option[ConnektRequest] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val colFamiliesReqd = List("r", "c", "t")
      val rawData = fetchRow(hTableName, connektId, colFamiliesReqd)

      val reqProps = rawData.get("r")
      val reqChannelInfoProps = rawData.get("c")
      val reqChannelDataProps = rawData.get("t")

      val allProps = reqProps.flatMap[Map[String, Array[Byte]]](r => reqChannelInfoProps.map[Map[String, Array[Byte]]](m => m ++ r))

      allProps.map(fields => {
        val channelReqInfo = reqChannelInfoProps.map(getChannelRequestInfo).orNull
        val channelReqData = reqChannelDataProps.map(getChannelRequestData).orNull

        ConnektRequest(
          id = connektId,
          channel = fields.getS("channel"),
          sla = fields.getS("sla"),
          templateId = fields.getS("templateId"),
          scheduleTs = fields.getL("scheduleTs").asInstanceOf[Long],
          expiryTs = fields.getL("expiryTs").asInstanceOf[Long],
          channelInfo = channelReqInfo,
          channelData = channelReqData,
          meta = Map[String, String]()
        )
      })

    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching Request info failed for $connektId, ${e.getMessage}", e)
        throw new IOException(s"Fetching RequestInfo failed for $connektId", e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def updateRequestStatus(id: String, status: ChannelRequestData) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val statusPropsMap = channelRequestDataMap(status)
      val rawData = Map[String, Map[String, Array[Byte]]]("t" -> statusPropsMap)
      addRow(hTableName, id, rawData)

      ConnektLogger(LogFile.DAO).info(s"Request status updated for $id")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Request status update failed for $id ${e.getMessage}", e)
        throw new IOException(s"Request status update failed for $id", e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def fetchRequestInfo(id: String): Option[ChannelRequestInfo] = {
    implicit val hTableInterface = hTableFactory.getTableInterface(tableName)
    try {
      val colFamiliesReqd = List("c")
      val rawData = fetchRow(hTableName, id, colFamiliesReqd)

      val reqProps = rawData.get("c")
      reqProps.map(getChannelRequestInfo)

    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching Request info failed for $id, ${e.getMessage}", e)
        throw new IOException(s"Fetching RequestInfo failed for $id", e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }
}