/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.ResourcePriv
import com.flipkart.connekt.commons.entities.UserType.UserType

/**
 * @author aman.shrivastava on 11/12/15.
 */
trait TPrivDao {
  def getPrivileges(identifier: String , _type : UserType) : Option[ResourcePriv]

  def addPrivileges(identifier: String , _type : UserType, privs:List[String])

  def removePrivileges(identifier: String , _type : UserType, privs:List[String])


}
