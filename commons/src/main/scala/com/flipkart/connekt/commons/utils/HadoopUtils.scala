/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.utils

import org.apache.hadoop.hbase.util.Bytes

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
