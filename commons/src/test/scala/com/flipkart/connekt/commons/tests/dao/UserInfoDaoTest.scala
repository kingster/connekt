package com.flipkart.connekt.commons.tests.dao

import java.util.{UUID, Properties}

import com.flipkart.connekt.commons.dao.UserInfo
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.MySQLFactoryWrapper
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.typesafe.config.{Config, ConfigFactory}

/**
 * @author aman.shrivastava on 11/12/15.
 */
class UserInfoDaoTest extends ConnektUTSpec {
  val table = "USER_INFO"
  implicit var mysqlFactoryWrapper = new MySQLFactoryWrapper("w3-comm-db01.stage.ch.flipkart.com", "fk-pf-connekt", "root", "", getPoolProps)
  val id = UUID.randomUUID().toString
  val user = new AppUser(id, UUID.randomUUID().toString, "bro,commsvc", 345678875, "aman.s")


  private def getPoolProps: Config = {
    val props = new Properties()
    props.setProperty("maxIdle", "3")
    props.setProperty("initialSize", "3")
    props.setProperty("maxActive", "20")
    ConfigFactory.parseProperties(props)
  }

  "UserInfoDao test" should "add user info" in {
    new UserInfo(table, mysqlFactoryWrapper).addUserInfo(user) shouldEqual true
  }

  "UserInfoDao test" should "get user info" in {
    new UserInfo(table, mysqlFactoryWrapper).getUserInfo(id).get shouldEqual user
  }
}
