package com.flipkart.connekt.commons.tests.dao

import java.util.UUID

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * @author aman.shrivastava on 11/12/15.
 */
class UserInfoDaoTest extends CommonsBaseTest {
  val id = UUID.randomUUID().toString
  val user = new AppUser(id, UUID.randomUUID().toString, "bro,commsvc", 345678875, "aman.s")

  "UserInfoDao test" should "add user info" in {
    val userDao = DaoFactory.getUserInfoDao
    noException should be thrownBy  userDao.addUserInfo(user)
  }

  "UserInfoDao test" should "get user info" in {
    val userDao = DaoFactory.getUserInfoDao
    userDao.getUserInfo(id).get shouldEqual user
  }
}
