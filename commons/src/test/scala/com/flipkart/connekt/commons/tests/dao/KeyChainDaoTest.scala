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
package com.flipkart.connekt.commons.tests.dao

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.Key
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class KeyChainDaoTest extends CommonsBaseTest {

  val id = UUID.randomUUID().toString
  val key1 = new Key(UUID.randomUUID().toString.take(6), "ios", "dataValue".getBytes, new Date(), new Date(), null)
  val key2 = new Key(UUID.randomUUID().toString.take(6), "ios", "dataValue".getBytes, new Date(), new Date(), null)
  val key3 = new Key(UUID.randomUUID().toString.take(6), "windows", "dataValue".getBytes, new Date(), new Date(), null)
  val keys = List[Key](key1, key2, key3)

  "KeyChainDao test" should "add random data" in {
    val storageDao = DaoFactory.getKeyChainDao
    keys.foreach(
      noException should be thrownBy storageDao.put(_)
    )
  }

  "KeyChainDao test" should "get stored data" in {
    val storageDao = DaoFactory.getKeyChainDao
    noException should be thrownBy storageDao.get(key1.keyName)
    new String(storageDao.get(key1.keyName).get.value) shouldEqual "dataValue"
  }

  "KeyChainDao test" should "get list of keys" in {
    assert(DaoFactory.getKeyChainDao.getKeys("ios").size >= keys.count(_.kind == "ios"))
  }
}
