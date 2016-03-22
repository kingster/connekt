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

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao.ColumnData
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{CallbackEvent, ChannelRequestInfo}
import com.roundeights.hasher.Implicits._
import org.apache.hadoop.hbase.client.BufferedMutator

import scala.util.{Success, Try}

abstract class CallbackDao(tableName: String, hTableFactory: HTableFactory) extends TCallbackDao with HbaseDao {
  private val hTableConnFactory = hTableFactory
  private val hTableName = tableName
  private implicit lazy val hTableMutator: BufferedMutator = hTableFactory.getBufferedMutator(tableName)

  val columnFamily: String = "e"

  def channelEventPropsMap(channelCallbackEvent: CallbackEvent): ColumnData

  def mapToChannelEvent(channelEventPropsMap: Map[String, Array[Byte]]): CallbackEvent

  override def close() = {
    Option(hTableMutator).foreach(_.close())
  }

  def asyncSaveCallbackEvent(requestId: String, forContact: String, eventId: String, callbackEvent: CallbackEvent): Try[String] = {
    try {
      val channelEventProps = channelEventPropsMap(callbackEvent)
      val rawData = Map[String,ColumnData](columnFamily -> channelEventProps)
      val rowKey = s"${forContact.sha256.hash.hex}:$requestId:$eventId"
      asyncAddRow(rowKey, rawData)

      ConnektLogger(LogFile.DAO).info(s"Event details async-persisted for $requestId")
      Success(requestId)
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Event details async-persistence failed for $requestId ${e.getMessage}", e)
        throw new IOException("Event details async-persistence failed for %s".format(requestId), e)
    }
  }

  override def saveCallbackEvent(requestId: String, forContact: String, eventId: String, callbackEvent: CallbackEvent): Try[String] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val channelEventProps = channelEventPropsMap(callbackEvent)
      val rawData = Map[String,ColumnData](columnFamily -> channelEventProps)
      val rowKey = s"${forContact.sha256.hash.hex}:$requestId:$eventId"
      addRow(rowKey, rawData)

      ConnektLogger(LogFile.DAO).info(s"Event details persisted for $requestId")
      Success(requestId)
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Event details persistence failed for $requestId ${e.getMessage}", e)
        throw new IOException("Event details persistence failed for %s".format(requestId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  def fetchCallbackEvents(requestId: String, contactId: String, timestampRange: Option[(Long, Long)], maxRowsLimit: Option[Int]): List[CallbackEvent] = {
    val colFamiliesReqd = List(columnFamily)
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val rawDataList = fetchRows(s"${contactId.sha256.hash.hex}:$requestId", s"${contactId.sha256.hash.hex}:$requestId{", colFamiliesReqd, timestampRange, maxRowsLimit)
      rawDataList.values.flatMap(rowData => {
        val eventProps = rowData.get(columnFamily)
        eventProps.map(mapToChannelEvent)
      }).toList

    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching events trail failed for $requestId _ $contactId, ${e.getMessage}", e)
        throw new IOException(s"Fetching events trail failed for $requestId _ $contactId", e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  def deleteCallbackEvents(requestId: String, forContact: String): List[CallbackEvent] = {
    val colFamiliesReqd = List(columnFamily)
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val rows = fetchRows(s"${forContact.sha256.hash.hex}:$requestId", s"${forContact.sha256.hash.hex}:$requestId{", colFamiliesReqd)
      ConnektLogger(LogFile.DAO).info(s"Deleting events for $forContact:$requestId")
      rows.keySet.foreach(removeRow)
      rows.values.flatMap(rowData => {
        val eventProps = rowData.get(columnFamily)
        eventProps.map(mapToChannelEvent)
      }).toList
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Delete events failed for $requestId _ $forContact, ${e.getMessage}", e)
        throw new IOException(s"Delete events failed for $requestId _ $forContact", e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }

  def fetchCallbackEvents(requestId: String, event: ChannelRequestInfo, fetchRange: Option[(Long, Long)]): Map[String, List[CallbackEvent]]

  def fetchEventMapFromList(event: List[CallbackEvent]): Map[String, List[CallbackEvent]]

}
