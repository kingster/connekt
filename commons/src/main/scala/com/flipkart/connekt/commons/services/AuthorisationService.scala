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
    LocalCacheManager.getCache(LocalCacheType.ResourcePriv).get[Option[ResourcePriv]](key) match {
      case Some(p: Option[ResourcePriv]) => p
      case None =>
        try {
          val accessPrivilege = privDao.getPrivileges(identifier, level)
          LocalCacheManager.getCache(LocalCacheType.ResourcePriv).put[Option[ResourcePriv]](key, accessPrivilege)
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

  def getGroups(userName: String): List[String] = {
    LocalCacheManager.getCache(LocalCacheType.UserGroups).get[List[String]](userName).getOrElse {
      val userDomainGroup = if(userName.contains("@")) Some(userName.split("@")(1)) else None
      val userGroups = userInfoDao.getUserInfo(userName).map(_.getUserGroups).getOrElse(List.empty)

      val userAllGroups = userGroups ::: userDomainGroup.toList
      LocalCacheManager.getCache(LocalCacheType.UserGroups).put(userName, userAllGroups)
      userAllGroups.toList
    }
  }

  override def getAllPrivileges(userName: String): List[String] = {
    val userPrivs = getUserPrivileges(userName)
    val groupPrivs =  getGroups(userName).flatMap(getGroupPrivileges)
    userPrivs ++ groupPrivs ++ globalPrivileges
  }

  override def isAuthorized(username: String, resource: String*): Try[Boolean] = {
    try {
      Success(getAllPrivileges(username).intersect(resource.map(_.toUpperCase)).nonEmpty)
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
