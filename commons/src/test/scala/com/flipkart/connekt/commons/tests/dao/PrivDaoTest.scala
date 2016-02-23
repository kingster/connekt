package com.flipkart.connekt.commons.tests.dao

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{UserType, ResourcePriv}
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils

/**
 * @author aman.shrivastava on 11/12/15.
 */
class PrivDaoTest extends CommonsBaseTest {

  val id = StringUtils.generateRandomStr(8)
  val user = new ResourcePriv(id, UserType.USER, "READ,WRITE")



  "PrivDao test" should "add user info" in {
    val privDao = DaoFactory.getPrivDao
    noException should be thrownBy privDao.addPrivileges(user.userId, user.userType, user.resources.split(',').toList)
  }

  "PrivDao test" should "get priv info" in {
    val privDao = DaoFactory.getPrivDao
    val x = privDao.getPrivileges(id, UserType.USER).get
    x shouldEqual user

  }

  "PrivDao Test " should "remove priv" in {
    val privDao = DaoFactory.getPrivDao
    privDao.removePrivileges(user.userId, user.userType, List("WRITE"))
  }

  "PrivDao Test " should "get priv info" in {
    val privDao = DaoFactory.getPrivDao
    val newUser = new ResourcePriv(id, UserType.USER, "READ")
    privDao.getPrivileges(id, UserType.USER).get shouldEqual newUser
  }

}
