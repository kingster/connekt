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

import java.util.UUID

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{AppUserConfiguration, Channel}
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class UserConfigurationDaoTest extends CommonsBaseTest {

  val id = UUID.randomUUID().toString.substring(20)
  val userConfig = new AppUserConfiguration(
    id,
    Channel.PUSH,
    s"connekt-pn-$id",
    1000
  )

  lazy val dao = DaoFactory.getUserConfigurationDao

  "UserInfoDao test" should "add user info" in {
    noException should be thrownBy dao.addUserConfiguration(userConfig)
  }

  "UserInfoDao test" should "get user info" in {
    dao.getUserConfiguration(id,Channel.PUSH).get shouldEqual userConfig
  }
}
