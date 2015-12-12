package com.flipkart.connekt.commons.tests.dao

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{UserType, ResourcePriv}
import com.flipkart.connekt.commons.tests.BaseCommonsTest

/**
 * @author aman.shrivastava on 11/12/15.
 */
class PrivDaoTest extends BaseCommonsTest {

  val id = "kinshuk.bairagi"
  val user = new ResourcePriv(id, UserType.USER, "read,write")



  "PrivDao test" should "add user info" in {
    val privDao = DaoFactory.getPrivDao
    noException should be thrownBy privDao.addPrivileges(user.userId, user.userType, user.resources.split(',').toList)
  }

  "PrivDao test" should "get priv info" in {
    val privDao = DaoFactory.getPrivDao
    privDao.getPrivileges(id, UserType.USER).get shouldEqual user
  }

  "PrivDao Test " should "remove priv" in {
    val privDao = DaoFactory.getPrivDao
    privDao.removePrivileges(user.userId, user.userType, List("write"))
  }

  "PrivDao Test " should "get priv info" in {
    val privDao = DaoFactory.getPrivDao
    val newUser = new ResourcePriv(id, UserType.USER, "read")
    privDao.getPrivileges(id, UserType.USER).get shouldEqual newUser
  }

}
