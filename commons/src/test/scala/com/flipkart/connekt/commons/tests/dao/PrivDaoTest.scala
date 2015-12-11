package com.flipkart.connekt.commons.tests.dao

import java.util.Properties

import com.flipkart.connekt.commons.dao.PrivDao
import com.flipkart.connekt.commons.entities.{UserType, ResourcePriv}
import com.flipkart.connekt.commons.factories.MySQLFactoryWrapper
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.typesafe.config.{Config, ConfigFactory}

/**
 * @author aman.shrivastava on 11/12/15.
 */
class PrivDaoTest extends ConnektUTSpec {
  val table = "RESOURCE_PRIV"
  implicit var mysqlFactoryWrapper = new MySQLFactoryWrapper("w3-comm-db01.stage.ch.flipkart.com", "fk-pf-connekt", "root", "", getPoolProps)
  val id = "aman.shrivastava"
  val user = new ResourcePriv(id, UserType.USER, "read, write")


  private def getPoolProps: Config = {
    val props = new Properties()
    props.setProperty("maxIdle", "3")
    props.setProperty("initialSize", "3")
    props.setProperty("maxActive", "20")
    ConfigFactory.parseProperties(props)
  }

//  "UserInfoDao test" should "add user info" in {
//    new UserInfo(table, mysqlFactoryWrapper).addUserInfo(user) shouldEqual true
//  }

  "PrivDaoTest test" should "get priv info" in {
    new PrivDao(table, mysqlFactoryWrapper).getPrivileges(id, "USER").get shouldEqual user
//    new UserInfo(table, mysqlFactoryWrapper).getUserInfo(id).get shouldEqual user
  }
}
