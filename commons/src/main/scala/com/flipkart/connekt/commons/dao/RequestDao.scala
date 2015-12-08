package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ChannelStatus, ChannelRequestData, ConnektRequest}
/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
abstract class RequestDao(tableName: String, hTableFactory: HTableFactory) extends TRequestDao with HbaseDao {
  private val hTableConnFactory = hTableFactory
  private val hTableName = tableName

  protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]]

  protected def getChannelRequestData(dataMap: Map[String, Array[Byte]]): ChannelRequestData

  protected def channelStatusMap(channelStatus: ChannelStatus): Map[String, Array[Byte]]

  protected def getChannelStatus(statusMap: Map[String, Array[Byte]]): ChannelStatus

  override def saveRequestInfo(requestId: String, request: ConnektRequest) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val deviceRegInfoCfProps = Map[String, Array[Byte]](
        "id" -> requestId.getUtf8Bytes,
        "channel" -> request.channel.getUtf8Bytes,
        "sla" -> request.sla.getUtf8Bytes,
        "templateId" -> request.templateId.getUtf8Bytes,
        "scheduleTs" -> request.scheduleTs.getBytes,
        "expiryTs" -> request.expiryTs.getBytes
      )

      val channelRequestDataProps = channelRequestDataMap(request.channelData)
      val channelStatusProps = channelStatusMap(request.channelStatus)

      val rawData = Map[String, Map[String, Array[Byte]]]("r" -> deviceRegInfoCfProps, "c" -> channelRequestDataProps, "t" -> channelStatusProps)
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

  override def fetchRequestInfo(connektId: String): Option[ConnektRequest] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val colFamiliesReqd = List("r", "c", "t")
      val rawData = fetchRow(hTableName, connektId, colFamiliesReqd)

      val devRegProps = rawData.get("r")
      val devMetaProps = rawData.get("c")
      val channelStatusProps = rawData.get("t")

      val allProps = devRegProps.flatMap[Map[String, Array[Byte]]](r => devMetaProps.map[Map[String, Array[Byte]]](m => m ++ r))

      allProps.map(fields => {
        val channelRequestData = devMetaProps.map(getChannelRequestData).orNull
        val channelStatusData = channelStatusProps.map(getChannelStatus).orNull

        ConnektRequest(
          id = connektId,
          channelStatus = channelStatusData,
          channel = fields.getS("channel"),
          sla = fields.getS("sla"),
          templateId = fields.getS("templateId"),
          scheduleTs = fields.getL("scheduleTs").asInstanceOf[Long],
          expiryTs = fields.getL("expiryTs").asInstanceOf[Long],
          channelData = channelRequestData,
          meta = Map[String, String]()
        )
      })

    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching Request info failed for $connektId, ${e.getMessage}", e)
        throw new IOException("Fetching RequestInfo failed for %s".format(connektId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def updateRequestStatus(id: String, status: ChannelStatus) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val statusPropsMap = channelStatusMap(status)
      val rawData = Map[String, Map[String, Array[Byte]]]("t" -> statusPropsMap)
      addRow(hTableName, id, rawData)

      ConnektLogger(LogFile.DAO).info(s"Request status updated for $id")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Request status update failed for $id ${e.getMessage}", e)
        throw new IOException("Request status update failed for %s".format(id), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }
}