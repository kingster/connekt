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

  def canEqual(other: Any): Boolean = other.isInstanceOf[ResourcePriv]

  override def equals(other: Any): Boolean = other match {
    case that: ResourcePriv =>
      (that canEqual this) &&
        userId == that.userId &&
        userType == that.userType &&
        resources == that.resources
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(userId, userType, resources)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object UserType extends Enumeration {
  val GLOBAL, GROUP, USER = Value
}
