package com.flipkart.connekt.commons.dao

import java.io.IOException

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.commons.codec.CharEncoding
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.{KeyOnlyFilter, FilterList, PrefixFilter}
import org.apache.hadoop.hbase.util.Bytes

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
 *
 *
 * @author durga.s
 * @version 11/18/15
 */
trait HbaseDao {

  val emptyRowData = Map[String, Map[String, Array[Byte]]]("d" -> Map("empty" -> Bytes.toBytes(1)))

  @throws[IOException]
  def addRow(tableName: String, rowKey: String, data: Map[String, Map[String, Array[Byte]]])(implicit hTableInterface: HTableInterface) = {

    val put: Put = new Put(rowKey.getBytes(CharEncoding.UTF_8))
    data.foreach { case (colFamily, v) =>
      v.foreach { case (colQualifier, d) =>
        put.add(colFamily.getBytes(CharEncoding.UTF_8), colQualifier.getBytes(CharEncoding.UTF_8), d)
      }
    }

    hTableInterface.put(put)
  }

  @throws[IOException]
  def fetchRow(tableName: String, rowKey: String, colFamilies: List[String])(implicit hTableInterface: HTableInterface): Map[String, Map[String, Array[Byte]]] = {

    val get: Get = new Get(rowKey.getBytes(CharEncoding.UTF_8))
    colFamilies.foreach(cF => get.addFamily(cF.getBytes(CharEncoding.UTF_8)))

    val rowResult = hTableInterface.get(get)
    var resultMap = Map[String, Map[String, Array[Byte]]]()

    colFamilies.foreach { cF =>
      val optResult = rowResult.getFamilyMap(cF.getBytes(CharEncoding.UTF_8))

      Option(optResult).map(cFResult => {
        val i = cFResult.keySet().iterator()
        val vMap = scala.collection.mutable.Map[String, Array[Byte]]()

        while (i.hasNext) {
          val colQualifier = i.next
          vMap += new String(colQualifier) -> cFResult.get(colQualifier)
        }

        resultMap += cF -> vMap.toMap
      })
    }

    resultMap
  }

  def fetchRowKeys(tableName: String, rowStartKeyPrefix: String,rowStopKeyPrefix: String, colFamilies: List[String], timeRange: Option[(Long,Long)] = None)(implicit hTableInterface: HTableInterface):List[String] = {
    val scan = new Scan()
    scan.setStartRow(rowStartKeyPrefix.getBytes(CharEncoding.UTF_8))
    scan.setStopRow(rowStopKeyPrefix.getBytes(CharEncoding.UTF_8))

    if(timeRange.isDefined )
      scan.setTimeRange(timeRange.get._1, timeRange.get._2)

    val filters = new FilterList()
    filters.addFilter(new KeyOnlyFilter())
    scan.setFilter(filters)

    val resultScanner = hTableInterface.getScanner(scan)
    val ri = resultScanner.iterator()

    var results = ListBuffer[String]()
    while (ri.hasNext) {
      results += Bytes.toString(ri.next().getRow)
    }

    results.toList
  }

  def fetchRows(tableName: String, rowStartKeyPrefix: String,rowStopKeyPrefix: String, colFamilies: List[String])(implicit hTableInterface: HTableInterface): List[Map[String, Map[String, Array[Byte]]]] = {
    val scan = new Scan()
    scan.setStartRow(rowStartKeyPrefix.getBytes(CharEncoding.UTF_8))
    scan.setStartRow(rowStopKeyPrefix.getBytes(CharEncoding.UTF_8))

    val resultScanner = hTableInterface.getScanner(scan)
    val rList = new ListBuffer[Map[String, Map[String, Array[Byte]]]]()

    var ri = resultScanner.iterator()
    while (ri.hasNext) {

      var resultMap = Map[String, Map[String, Array[Byte]]]()

      colFamilies.foreach { cF =>
        val optResult = ri.next().getFamilyMap(cF.getBytes(CharEncoding.UTF_8))

        Option(optResult).map(cFResult => {
          val i = cFResult.keySet().iterator()
          val vMap = scala.collection.mutable.Map[String, Array[Byte]]()

          while (i.hasNext) {
            val colQualifier = i.next
            vMap += new String(colQualifier) -> cFResult.get(colQualifier)
          }

          resultMap += cF -> vMap.toMap
        })
      }

      rList += resultMap
    }

    resultScanner.close()
    rList.toList
  }

  def fetchAllRows(tableName: String, rowStartKeyPrefix: String,rowStopKeyPrefix: String, colFamilies: List[String], minStamp: Long, maxStamp: Long)(implicit hTableInterface: HTableInterface): List[(String, Map[String, Map[String, Array[Byte]]])] = {
    val scan = new Scan()
    scan.setStartRow(rowStartKeyPrefix.getBytes(CharEncoding.UTF_8))
    scan.setStartRow(rowStopKeyPrefix.getBytes(CharEncoding.UTF_8))
    scan.setTimeRange(minStamp, maxStamp)

    val resultScanner = hTableInterface.getScanner(scan)
    val rList = new ListBuffer[(String, Map[String, Map[String, Array[Byte]]])]()

    val ri = resultScanner.iterator()
    while (ri.hasNext) {
      var resultMap = Map[String, Map[String, Array[Byte]]]()
      val riNext = ri.next()

      colFamilies.foreach { cF =>
        val optResult = riNext.getFamilyMap(cF.getBytes(CharEncoding.UTF_8))

        Option(optResult).map(cFResult => {
          val i = cFResult.keySet().iterator()
          val vMap = scala.collection.mutable.Map[String, Array[Byte]]()

          while (i.hasNext) {
            val colQualifier = i.next
            vMap += new String(colQualifier) -> cFResult.get(colQualifier)
          }

          resultMap += cF -> vMap.toMap
        })
      }

      rList += ((riNext.getRow.toString, resultMap))
    }

    resultScanner.close()
    rList.toList
  }

}

object HbaseDao {

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
    def getS(key: String) = m.get(key).map(_.getString).orNull

    def getB(key: String) = m.get(key).exists(_.getBoolean)

    def getL(key: String) = m.get(key).map(Bytes.toLong).getOrElse(null)

    def getKV(key: String) = m.get(key).map(_.getString).map(objMapper.readValue[ObjectNode]).orNull
  }

}

