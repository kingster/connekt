package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest}
/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
abstract class RequestDao(tableName: String, hTableFactory: HTableFactory) extends Dao with HbaseDao {
  private val hTableConnFactory = hTableFactory
  private val hTableName = tableName

  protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]]

  protected def getChannelRequestData(dataMap: Map[String, Array[Byte]]): ChannelRequestData

  def saveRequestInfo(requestId: String, request: ConnektRequest) = {
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

      val rawData = Map[String, Map[String, Array[Byte]]]("r" -> deviceRegInfoCfProps, "c" -> channelRequestDataProps)
      addRow(hTableName, requestId, rawData)

      ConnektLogger(LogFile.DAO).info(s"Request info persisted for $requestId")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).info(s"Request info persistence failed for $requestId ${e.getMessage}")
        throw new IOException("DeviceDetails registration failed for %s".format(requestId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  def fetchRequestInfo(connektId: String): Option[ConnektRequest] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val colFamiliesReqd = List("r", "c")
      val rawData = fetchRow(hTableName, connektId, colFamiliesReqd)

      val devRegProps = rawData.get("r")
      val devMetaProps = rawData.get("c")

      val allProps = devRegProps.flatMap[Map[String, Array[Byte]]](r => devMetaProps.map[Map[String, Array[Byte]]](m => m ++ r))

      allProps.map(fields => {
        val channelRequestData = devMetaProps.map(getChannelRequestData).orNull

        ConnektRequest(
          id = connektId,
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
}