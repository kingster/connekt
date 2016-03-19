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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ChannelRequestInfo, ConnektRequest}
import com.flipkart.connekt.commons.utils.StringUtils

abstract class RequestDao(tableName: String, hTableFactory: HTableFactory) extends TRequestDao with HbaseDao {
  private val hTableConnFactory = hTableFactory
  private val hTableName = tableName

  protected def channelRequestInfoMap(channelRequestInfo: ChannelRequestInfo): Map[String, Array[Byte]]

  protected def getChannelRequestInfo(reqInfoProps: Map[String, Array[Byte]]): ChannelRequestInfo

  protected def channelRequestDataMap(channelRequestData: ChannelRequestData): Map[String, Array[Byte]]

  protected def getChannelRequestData(reqDataProps: Map[String, Array[Byte]]): ChannelRequestData

  private def getChannelRequestModel(reqDataProps: Map[String, Array[Byte]]) = reqDataProps.getKV("model")

  private def channelRequestModel(requestModel: ObjectNode) = Map[String, Array[Byte]]("model" -> requestModel.toString.getUtf8Bytes)

  override def saveRequest(requestId: String, request: ConnektRequest) = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      var requestProps = Map[String, Array[Byte]](
        "id" -> requestId.getUtf8Bytes,
        "channel" -> request.channel.getUtf8Bytes,
        "sla" -> request.sla.getUtf8Bytes
      )

      request.expiryTs.foreach(requestProps += "expiryTs" -> _.getBytes)
      request.scheduleTs.foreach(requestProps += "scheduleTs" -> _.getBytes)

      val channelRequestInfoProps = channelRequestInfoMap(request.channelInfo)
      val channelRequestDataProps = Option(request.channelData).map(channelRequestDataMap).getOrElse(Map[String, Array[Byte]]())
      val channelRequestModelProps = Option(request.channelDataModel).map(channelRequestModel).getOrElse(Map[String, Array[Byte]]())

      val rawData = Map[String, Map[String, Array[Byte]]]("r" -> requestProps, "c" -> channelRequestInfoProps, "t" -> (channelRequestDataProps ++ channelRequestModelProps))
      addRow(requestId, rawData)

      ConnektLogger(LogFile.DAO).info(s"Request info persisted for $requestId")
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Request info persistence failed for $requestId ${e.getMessage}", e)
        throw new IOException("Request info persistence failed for %s".format(requestId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  override def fetchRequest(connektId: String): Option[ConnektRequest] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val colFamiliesReqd = List("r", "c", "t")
      val rawData = fetchRow(connektId, colFamiliesReqd)

      val reqProps = rawData.get("r")
      val reqChannelInfoProps = rawData.get("c")
      val reqChannelDataProps = rawData.get("t")

      val allProps = reqProps.flatMap[Map[String, Array[Byte]]](r => reqChannelInfoProps.map[Map[String, Array[Byte]]](m => m ++ r))

      allProps.map(fields => {
        val channelReqInfo = reqChannelInfoProps.map(getChannelRequestInfo).orNull
        val channelReqData = reqChannelDataProps.map(getChannelRequestData).orNull
        val channelReqModel = reqChannelDataProps.map(getChannelRequestModel).orNull

        ConnektRequest(
          id = connektId,
          channel = fields.getS("channel"),
          sla = fields.getS("sla"),
          templateId = Option(fields.getS("templateId")),
          scheduleTs = Option(fields.getL("scheduleTs").asInstanceOf[Long]),
          expiryTs = Option(fields.getL("expiryTs").asInstanceOf[Long]),
          channelInfo = channelReqInfo,
          channelData = channelReqData,
          channelDataModel = Option(channelReqModel).getOrElse(StringUtils.getObjectNode),
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
      addRow(id, rawData)

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
      val rawData = fetchRow(id, colFamiliesReqd)

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
