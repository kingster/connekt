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
package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class KeyChainServiceTest extends CommonsBaseTest {

  val keyData = UUID.randomUUID().toString.take(6)
  val keyValue = UUID.randomUUID().toString.take(6)

  "Storage Service" should "store key value" in {
    noException should be thrownBy ServiceFactory.getKeyChainService.put(keyData, keyValue)
  }

  "Storage Service" should "get value for key" in {
    noException should be thrownBy ServiceFactory.getKeyChainService.get(keyData)
    ServiceFactory.getKeyChainService.get(keyData).get.get shouldEqual keyValue.getBytes
  }

}
