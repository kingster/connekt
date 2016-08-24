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
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.flipkart.connekt.commons.dao.HbaseDao._
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.metrics.Timed
import org.apache.commons.codec.CharEncoding
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.{FilterList, KeyOnlyFilter}
import org.apache.hadoop.hbase.util.Bytes

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

trait HbaseDao extends Instrumented {

  lazy val scanFetchSize:Int = ConnektConfig.get("receptors.hbase.scan-fetch-size").getOrElse("100").toInt
  lazy val getBatchSize:Int = ConnektConfig.get("receptors.hbase.get-batch-size").getOrElse("100").toInt

  @throws[IOException]
  @Timed("asyncAdd")
  def asyncAddRow(rowKey: String, data: RowData)(implicit hMutator: BufferedMutator) = {

    val put: Put = new Put(rowKey.getBytes(CharEncoding.UTF_8))
    data.foreach { case (colFamily, v) =>
      v.foreach { case (colQualifier, d) =>
        put.addColumn(colFamily.getUtf8Bytes, colQualifier.getUtf8Bytes, d)
      }
    }

    hMutator.mutate(put)
  }

  @throws[IOException]
  @Timed("add")
  def addRow(rowKey: String, data: RowData)(implicit hTable: Table) = {

    val put: Put = new Put(rowKey.getBytes(CharEncoding.UTF_8))
    data.foreach { case (colFamily, v) =>
      v.foreach { case (colQualifier, d) =>
        put.addColumn(colFamily.getUtf8Bytes, colQualifier.getUtf8Bytes, d)
      }
    }

    hTable.put(put)
  }

  @throws[IOException]
  @Timed("remove")
  def removeRow(rowKey: String)(implicit hTable: Table): Unit = {
    val del = new Delete(rowKey.getUtf8Bytes)
    hTable.delete(del)
  }

  @throws[IOException]
  @Timed("get")
  def fetchRow(rowKey: String, colFamilies: List[String])(implicit hTable: Table): RowData = {

    val get: Get = new Get(rowKey.getBytes(CharEncoding.UTF_8))
    colFamilies.foreach(cF => get.addFamily(cF.getBytes(CharEncoding.UTF_8)))

    val rowResult = hTable.get(get)
    getRowData(rowResult, colFamilies)

  }

  @throws[IOException]
  @Timed("getKeys")
  def fetchRowKeys(rowStartKeyPrefix: String, rowStopKeyPrefix: String, colFamilies: List[String], timeRange: Option[(Long, Long)] = None)(implicit hTable: Table): List[String] = {
    val scan = new Scan()
    scan.setStartRow(rowStartKeyPrefix.getBytes(CharEncoding.UTF_8))
    scan.setStopRow(rowStopKeyPrefix.getBytes(CharEncoding.UTF_8))

    if (timeRange.isDefined)
      scan.setTimeRange(timeRange.get._1, timeRange.get._2)

    val filters = new FilterList()
    filters.addFilter(new KeyOnlyFilter())
    scan.setFilter(filters)

    val resultScanner = hTable.getScanner(scan)
    try {
      resultScanner.iterator().toIterator.map(_.getRow.getString).toList
    } finally {
      if (resultScanner != null)
        resultScanner.close()
    }
  }

  /**
   *
   * @param rowStartKeyPrefix
   * @param rowStopKeyPrefix
   * @param colFamilies
   * @param timeRange
   * @param maxRowLimit If not Defined, defaults to Int.MaxValue
   * @param hTable
   * @return
   */
  @throws[IOException]
  @Timed("scan")
  def fetchRows(rowStartKeyPrefix: String, rowStopKeyPrefix: String, colFamilies: List[String], timeRange: Option[(Long, Long)] = None, maxRowLimit: Option[Int] = None)(implicit hTable: Table): Map[String, HRowData] = {
      val scan = new Scan()
      scan.setStartRow(rowStartKeyPrefix.getBytes(CharEncoding.UTF_8))
      scan.setStopRow(rowStopKeyPrefix.getBytes(CharEncoding.UTF_8))

      if (timeRange.isDefined)
        scan.setTimeRange(timeRange.get._1, timeRange.get._2)

      scan.setCaching(scanFetchSize)

    val resultScanner = hTable.getScanner(scan)
    try {
      resultScanner.iterator().toIterator.take(maxRowLimit.getOrElse(Int.MaxValue))
        .map(rowResult => {
          rowResult.getRow.getString -> HRowData(getRowData(rowResult, colFamilies), rowResult.rawCells().head.getTimestamp)
        }).toMap
    } finally {
      if (resultScanner != null)
        resultScanner.close()
    }
  }

  @throws[IOException]
  @Timed("hmget")
  def fetchMultiRows(rowKeys: List[String], colFamilies: List[String])(implicit hTable: Table): Map[String, RowData] = {

    rowKeys
      .filter(rowKey => rowKey != null && rowKey.nonEmpty)
      .grouped(getBatchSize).map( groupedRowKeys => {
      val gets:List[Get] = groupedRowKeys.map(rowKey => {
        val get = new Get(rowKey.getBytes(CharEncoding.UTF_8))
        colFamilies.foreach(cF => get.addFamily(cF.getBytes(CharEncoding.UTF_8)))
        get
      })
      val rowResults = hTable.get(gets.toList.asJava)
      rowResults.filter(_.getRow != null).map(rowResult => rowResult.getRow.getString -> getRowData(rowResult, colFamilies)).toMap[String, RowData]
    }).reduceOption(_ ++ _).getOrElse(Map.empty[String, RowData])
  }


  protected def getRowData(result: Result, colFamilies: List[String]): RowData = {
    colFamilies.flatMap { cF =>
      val optResult = result.getFamilyMap(cF.getBytes(CharEncoding.UTF_8))
      Option(optResult).map(cFResult => {
        val cQIterator = cFResult.keySet().iterator()
        val cFData: ColumnData = cQIterator.asScala.map(colQualifier => colQualifier.getString -> cFResult.get(colQualifier)).toMap
        cF -> cFData
      })
    }.toMap
  }


}

object HbaseDao {

  type ColumnData = scala.collection.immutable.Map[String, Array[Byte]]
  // ColumnQualifer -> Data
  type RowData = scala.collection.immutable.Map[String, ColumnData] // ColumnFamily -> ColumnData

  private[connekt] case class HRowData(data: RowData, timestamp: Long)

  val emptyRowData = Map[String, ColumnData]("d" -> Map("empty" -> Bytes.toBytes(1)))

  val objMapper = new ObjectMapper() with ScalaObjectMapper
  objMapper.registerModules(Seq(DefaultScalaModule): _*)
  objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  implicit class stringHandyFunctions(val s: String) {
    def getUtf8Bytes = Bytes.toBytes(s)
  }

  implicit class longHandyFunctions(val l: Long) {
    def getBytes = Bytes.toBytes(l)
  }

  implicit class booleanHandyFunctions(val b: Boolean) {
    def getBytes = Bytes.toBytes(b)
  }

  implicit class byteArrayHandyFunctions(val b: Array[Byte]) {
    def getString = Bytes.toString(b)

    def getLong = Bytes.toLong(b)

    def getBoolean = Bytes.toBoolean(b)

    def getInt = Bytes.toInt(b)
  }

  implicit class mapKVHandyFunctions(val m: Map[String, Array[Byte]]) {
    def getS(key: String): String = m.get(key).map(_.getString).orNull

    def getB(key: String): Boolean = m.get(key).exists(_.getBoolean)

    def getL(key: String) = m.get(key).map(Bytes.toLong).getOrElse(null)

    def getKV(key: String): ObjectNode = m.get(key).map(_.getString).map(objMapper.readValue[ObjectNode]).orNull
  }

}
