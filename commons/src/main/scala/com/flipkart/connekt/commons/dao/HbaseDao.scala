package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.flipkart.connekt.commons.dao.HbaseDao._
import org.apache.commons.codec.CharEncoding
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.{FilterList, KeyOnlyFilter}
import org.apache.hadoop.hbase.util.Bytes

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 *
 *
 * @author durga.s
 * @version 11/18/15
 */
trait HbaseDao {

  @throws[IOException]
  def addRow(rowKey: String, data: RowData)(implicit hTableInterface: HTableInterface) = {

    val put: Put = new Put(rowKey.getBytes(CharEncoding.UTF_8))
    data.foreach { case (colFamily, v) =>
      v.foreach { case (colQualifier, d) =>
        put.add(colFamily.getBytes(CharEncoding.UTF_8), colQualifier.getBytes(CharEncoding.UTF_8), d)
      }
    }

    hTableInterface.put(put)
  }

  @throws[IOException]
  def removeRow(rowKey: String)(implicit hTableInterface: HTableInterface): Unit = {
    val del = new Delete(rowKey.getUtf8Bytes)
    hTableInterface.delete(del)
  }

  @throws[IOException]
  def fetchRow(rowKey: String, colFamilies: List[String])(implicit hTableInterface: HTableInterface): RowData = {

    val get: Get = new Get(rowKey.getBytes(CharEncoding.UTF_8))
    colFamilies.foreach(cF => get.addFamily(cF.getBytes(CharEncoding.UTF_8)))

    val rowResult = hTableInterface.get(get)
    getRowData(rowResult, colFamilies)

  }

  @throws[IOException]
  def fetchRowKeys(rowStartKeyPrefix: String, rowStopKeyPrefix: String, colFamilies: List[String], timeRange: Option[(Long, Long)] = None)(implicit hTableInterface: HTableInterface): List[String] = {
    val scan = new Scan()
    scan.setStartRow(rowStartKeyPrefix.getBytes(CharEncoding.UTF_8))
    scan.setStopRow(rowStopKeyPrefix.getBytes(CharEncoding.UTF_8))

    if (timeRange.isDefined)
      scan.setTimeRange(timeRange.get._1, timeRange.get._2)

    val filters = new FilterList()
    filters.addFilter(new KeyOnlyFilter())
    scan.setFilter(filters)

    val resultScanner = hTableInterface.getScanner(scan)
    val ri = resultScanner.iterator()

    var results = ListBuffer[String]()
    while (ri.hasNext) {
      results += ri.next().getRow.getString
    }

    results.toList
  }

  /**
   *
   * @param rowStartKeyPrefix
   * @param rowStopKeyPrefix
   * @param colFamilies
   * @param timeRange
   * @param maxRowLimit If not Defined, defaults to Int.MaxValue
   * @param hTableInterface
   * @return
   */
  @throws[IOException]
  def fetchRows(rowStartKeyPrefix: String, rowStopKeyPrefix: String, colFamilies: List[String], timeRange: Option[(Long, Long)] = None, maxRowLimit: Option[Int] = None)(implicit hTableInterface: HTableInterface): Map[String, RowData] = {

    val scan = new Scan()
    scan.setStartRow(rowStartKeyPrefix.getBytes(CharEncoding.UTF_8))
    scan.setStopRow(rowStopKeyPrefix.getBytes(CharEncoding.UTF_8))

    if (timeRange.isDefined)
      scan.setTimeRange(timeRange.get._1, timeRange.get._2)

    val resultScanner = hTableInterface.getScanner(scan)
    var rowMap = Map[String,RowData]()

    resultScanner.iterator().toIterator.take(maxRowLimit.getOrElse(Int.MaxValue))
      .foreach(rowResult => rowMap += rowResult.getRow.getString -> getRowData(rowResult, colFamilies))

    rowMap
  }

  @throws[IOException]
  def fetchMultiRows(rowKeys: List[String], colFamilies: List[String])(implicit hTableInterface: HTableInterface): Map[String, RowData] = {
    val gets = ListBuffer[Get]()
    rowKeys.map(rowKey => {
      val get = new Get(rowKey.getBytes(CharEncoding.UTF_8))
      colFamilies.foreach(cF => get.addFamily(cF.getBytes(CharEncoding.UTF_8)))
      gets += get
    })
    val rowResults = hTableInterface.get(gets.toList.asJava)
    var rowMap = Map[String,RowData]()
    rowResults.filter(_.getRow != null).foreach(rowResult => rowMap += rowResult.getRow.getString -> getRowData(rowResult, colFamilies))
    rowMap
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

  val emptyRowData = Map[String, ColumnData]("d" -> Map("empty" -> Bytes.toBytes(1)))

  val objMapper = new ObjectMapper() with ScalaObjectMapper
  objMapper.registerModules(Seq(DefaultScalaModule): _*)

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

