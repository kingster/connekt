/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.utils

object NullWrapper {

  val noData = Array[Byte]( 0x00 )

  implicit class NullWrap(data: Array[Byte]) {

    def wrap:Array[Byte] = {
      var payload:Array[Byte] = noData
      if(data!= null && data.nonEmpty) {
        payload = Array[Byte]( 0x01 ) ++ data
      }
      payload
    }
  }

  implicit class NullUnWrap(data: Array[Byte]) {

    def unwrap:Array[Byte] = {
      data.head match {
        case 0x00 =>
          Array[Byte]()
        case 0x01 =>
          data.tail
      }
    }
  }

}
