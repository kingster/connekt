package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.CallbackEvent

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

  def channelEventPropsMap(channelCallbackEvent: CallbackEvent): Map[String, Array[Byte]]

  def mapToChannelEvent(channelEventPropsMap: Map[String, Array[Byte]]): CallbackEvent

  override def saveCallbackEvent(requestId: String, callbackEvent: CallbackEvent): Try[String] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val channelEventProps = channelEventPropsMap(callbackEvent)
      val rawData = Map[String, Map[String, Array[Byte]]]("e" -> channelEventProps)
      addRow(hTableName, requestId, rawData)

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

  override def fetchCallbackEvent(requestId: String): Option[CallbackEvent] = {
    implicit val hTableInterface = hTableConnFactory.getTableInterface(hTableName)
    try {
      val colFamiliesReqd = List("e")
      val rawData = fetchRow(hTableName, requestId, colFamiliesReqd)

      val eventProps = rawData.get("e")
      eventProps.map(mapToChannelEvent)
    } catch {
      case e: IOException =>
        ConnektLogger(LogFile.DAO).error(s"Fetching Event details failed for $requestId, ${e.getMessage}", e)
        throw new IOException("Fetching Events failed for %s".format(requestId), e)
    } finally {
      hTableConnFactory.releaseTableInterface(hTableInterface)
    }
  }
}
