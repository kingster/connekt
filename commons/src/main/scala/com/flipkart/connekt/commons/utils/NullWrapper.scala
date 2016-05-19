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
package com.flipkart.connekt.commons.utils

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

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
        case _ =>
          //This is crazy, but there are some crazy old data which
          // wasn't null-wapped and now is tried be unwrapped causing this
          ConnektLogger(LogFile.SERVICE).warn(s"Non-NullWrapped Element being unwrapped, data [${data.toList}]")
          data
      }
    }
  }

}
