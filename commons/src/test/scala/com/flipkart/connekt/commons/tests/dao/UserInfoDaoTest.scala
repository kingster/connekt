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
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class UserInfoDaoTest extends CommonsBaseTest {
  val id = UUID.randomUUID().toString
  val apiKey = UUID.randomUUID().toString
  val user = new AppUser(id, apiKey, "bro,commsvc","bro-")

  "UserInfoDao test" should "add user info" in {
    val userDao = DaoFactory.getUserInfoDao
    noException should be thrownBy  userDao.addUserInfo(user)
  }

  "UserInfoDao test" should "get user info" in {
    val userDao = DaoFactory.getUserInfoDao
    userDao.getUserInfo(id).get shouldEqual user
  }

  "UserInfoDao test" should "get user info for apiKey" in {
    val userDao = DaoFactory.getUserInfoDao
    userDao.getUserByKey(apiKey).get shouldEqual user
  }
}
