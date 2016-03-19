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
package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.entities.UserType.UserType

import scala.util.Try

/**
 * @author aman.shrivastava on 12/12/15.
 */
trait TAuthorisationService extends TService {

  def isAuthorized(username: String,resource: String*): Try[Boolean]
  def removeAuthorization(userId: String, userType: UserType, resources: List[String]): Try[Unit]
  def addAuthorization(userId: String, userType: UserType, resources: List[String]): Try[Unit]
  def getGroupPrivileges(groupName: String): List[String]
  def getUserPrivileges(userName: String): List[String]
}
