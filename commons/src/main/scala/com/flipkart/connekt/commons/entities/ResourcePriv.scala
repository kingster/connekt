package com.flipkart.connekt.commons.entities

import javax.persistence.Column

/**
 *
 *
 * @author durga.s
 * @version 12/11/15
 */
class ResourcePriv {

  @Column(name = "userId")
  var userId: String = _

  @Column(name = "userType")
  var userType: UserType.Value = _

  @Column(name = "resources")
  var resources: String = _

  def this(userId: String, userType: UserType.Value, resources: String) {
    this()
    this.userId = userId
    this.userType = userType
    this.resources = resources
  }
}

object UserType extends Enumeration {
  val GLOBAL, GROUP, USER = Value
}
