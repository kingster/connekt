package com.flipkart.connekt.commons.utils

import org.apache.hadoop.hbase.util.Bytes

/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
object HadoopUtils {
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
}
