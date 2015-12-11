package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.ResourcePriv

/**
 * @author aman.shrivastava on 11/12/15.
 */
trait TPrivDao {
  def getPrivileges(identifier: String , _type : String) : Option[ResourcePriv]

  def setPrivileges(identifier: String, _type: String, resources: String): Boolean

}
