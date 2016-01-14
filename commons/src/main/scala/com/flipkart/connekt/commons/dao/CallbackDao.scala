package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.dao.HbaseDao.ColumnData
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ChannelRequestInfo, CallbackEvent}

import scala.collection.mutable.ListBuffer
import scala.util.{Success, Try}

/**
 *
 *
 * @author durga.s
 * @version 12/9/15
 */
abstract class CallbackDao(tableName: String, hTableFactory: HTableFactory) extends TCallbackDao with HbaseDao {
  private val hTableConnFactory = hTableFactory
  private val hTableName = tableName

  val columnFamily:String = "e"

  def channelEventPropsMap(channelCallbackEvent: CallbackEvent): ColumnData

  def mapToChannelEvent(channelEventPropsMap: Map[String, Array[Byte]]): CallbackEvent

  override def saveCallbackEvent(requestId: String, forContact: String, eventId: String, callbackEvent: CallbackEvent): Try[String] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val channelEventProps = channelEventPropsMap(callbackEvent)
      val rawData = Map[String,ColumnData](columnFamily -> channelEventProps)
      val rowKey = s"$forContact:$requestId:$eventId"
      addRow(hTableName, rowKey, rawData)

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

  def fetchCallbackEvents(requestId: String, contactId: String): List[CallbackEvent] = {
    val colFamiliesReqd = List(columnFamily)
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {

      val rawDataList = fetchRows(hTableName, s"$contactId:$requestId", s"$contactId:$requestId{", colFamiliesReqd)
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

  def fetchCallbackEvents(requestId: String, event: ChannelRequestInfo): List[CallbackEvent]
  def fetchEventMapFromList(event: List[CallbackEvent]): Map[String, List[CallbackEvent]]
}
