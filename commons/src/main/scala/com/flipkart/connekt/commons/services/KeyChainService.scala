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
package com.flipkart.connekt.commons.services

import java.util.Date

import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.TKeyChainDao
import com.flipkart.connekt.commons.entities.Key
import com.flipkart.connekt.commons.metrics.Instrumented

import scala.util.Try

class KeyChainService(dao: TKeyChainDao) extends TStorageService with Instrumented {

  override def put(key: String, value: String): Try[Unit] = Try_ {
    dao.put(new Key(key, "STRING", value.getBytes, new Date(), new Date(), null))
  }

  override def put(key: String, value: Array[Byte]): Try[Unit] = Try_ {
    dao.put(new Key(key, "BYTE_ARRAY", value, new Date(), new Date(), null))
  }

  override def get(key: String): Try[Option[Array[Byte]]] = Try_ {
    dao.get(key).map(_.value)
  }

}
