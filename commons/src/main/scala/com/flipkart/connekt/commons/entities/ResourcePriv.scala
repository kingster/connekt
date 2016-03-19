/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.entities

import javax.persistence.Column

import com.flipkart.connekt.commons.entities.UserType.UserType

class ResourcePriv {

  @Column(name = "userId")
  var userId: String = _

  @EnumTypeHint(value = "com.flipkart.connekt.commons.entities.UserType")
  @Column(name = "userType")
  var userType: UserType = UserType.USER

  @Column(name = "resources")
  var resources: String = _

  def this(userId: String, userType: UserType, resources: String) {
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
  type UserType = Value
  val GLOBAL, GROUP, USER = Value
}
