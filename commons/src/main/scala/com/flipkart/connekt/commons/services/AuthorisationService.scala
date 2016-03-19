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

import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.{PrivDao, TUserInfo}
import com.flipkart.connekt.commons.entities.UserType.UserType
import com.flipkart.connekt.commons.entities.{ResourcePriv, UserType}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}

import scala.util.{Failure, Success, Try}
/**
 * @author aman.shrivastava on 12/12/15.
 */

class AuthorisationService(privDao: PrivDao, userInfoDao: TUserInfo) extends TAuthorisationService with SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.AUTH_CHANGE))

  private lazy val globalPrivileges = {
    read("*", UserType.GLOBAL) match {
      case Some(priv) =>
        priv.resources.split(',').toList
      case None =>
        List()
    }
  }

  private def cacheKey(identifier: String, level: UserType): String = identifier + level.toString

  private def removeCache(identifier: String, level: UserType): Unit = {
    val key = cacheKey(identifier, level)
    LocalCacheManager.getCache(LocalCacheType.ResourcePriv).remove(key)
  }

  private def read(identifier: String, level: UserType): Option[ResourcePriv] = {
    val key = cacheKey(identifier, level)
    LocalCacheManager.getCache(LocalCacheType.ResourcePriv).get[ResourcePriv](key) match {
      case p: Some[ResourcePriv] => p
      case None =>
        try {
          val accessPrivilege = privDao.getPrivileges(identifier, level)
          accessPrivilege.foreach(p => LocalCacheManager.getCache(LocalCacheType.ResourcePriv).put[ResourcePriv](key, p))
          accessPrivilege
        } catch {
          case e: Exception =>
            throw e
        }
    }
  }

  override def getGroupPrivileges(groupName: String): List[String] = {
    read(groupName, UserType.GROUP).map(_.resources.split(',').toList).getOrElse(List())
  }

  override def getUserPrivileges(userName: String): List[String] = {
    read(userName, UserType.USER).map(_.resources.split(',').toList).getOrElse(List())
  }

  override def isAuthorized(username: String, resource: String*): Try[Boolean] = {
    try {
      val userPrivs = getUserPrivileges(username)
      val groupPrivs = userInfoDao.getUserInfo(username).map(_.groups.split(',').map(_.trim)).get.flatMap(getGroupPrivileges)
      val allowedPrivileges = (userPrivs ++ groupPrivs ++ globalPrivileges).toSet

      Success(allowedPrivileges.intersect(resource.toSet[String].map(_.toUpperCase)).nonEmpty)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Error isAuthorized user [$username] info: ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def removeAuthorization(userId: String, userType: UserType, resources: List[String]): Try[Unit] = {
    try {
      privDao.removePrivileges(userId, userType, resources)
      removeCache(userId, userType)
      Success(Unit)
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }

  override def addAuthorization(userId: String, userType: UserType, resources: List[String]): Try[Unit] = {
    try {
      privDao.addPrivileges(userId, userType, resources)
      removeCache(userId, userType)
      Success(Unit)
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Unit = {
    _type match {
      case SyncType.AUTH_CHANGE => Try_ {
        removeCache(args.head.toString, UserType.withName(args.last.toString))
      }
      case _ =>
    }
  }
}
