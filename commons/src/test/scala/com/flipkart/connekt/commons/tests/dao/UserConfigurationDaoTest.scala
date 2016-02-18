package com.flipkart.connekt.commons.tests.dao

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{Channel, AppUserConfiguration, AppUser}
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * @author aman.shrivastava on 11/12/15.
 */
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
