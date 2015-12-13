package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.{PrivDao, UserInfo}
import com.flipkart.connekt.commons.entities.UserType.UserType
import com.flipkart.connekt.commons.entities.{CacheManager, ResourcePriv, UserType}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}

import scala.util.{Failure, Success, Try}

/**
 * @author aman.shrivastava on 12/12/15.
 */

class AuthorisationService(privDao: PrivDao, userInfoDao: UserInfo) extends TAuthorisationService {

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
    CacheManager.getCache[Any].invalidate(key)
  }

  private def read(identifier: String, level: UserType): Option[ResourcePriv] = {
    var accessPrivilege: ResourcePriv = null
    val key = cacheKey(identifier, level)
    CacheManager.getCache[ResourcePriv].getIfPresent(key) match {
      case x: ResourcePriv => accessPrivilege = x
      case _ =>
        try {
          accessPrivilege = privDao.getPrivileges(identifier, level).orNull
          CacheManager.getCache[ResourcePriv].put(key, accessPrivilege)
        } catch {
          case e: Exception =>
            throw e
        }
    }
    Option(accessPrivilege)

  }

  private def getGroupPrivileges(groupName: String): List[String] = {
    read(groupName, UserType.GROUP).map(_.resources.split(',').toList).getOrElse(List())
  }

  private def getUserPrivileges(userName: String): List[String] = {
    read(userName, UserType.USER).map(_.resources.split(',').toList).getOrElse(List())
  }


  override def isAuthorized(tag: String, username: String): Try[Boolean] = {
    try {
      val userPrivs = getUserPrivileges(username)

      val groupPrivs = userInfoDao.getUserInfo(username).map(_.groups.split(',')).get.flatMap(getGroupPrivileges)
      val allowedPrivileges = (userPrivs ++ groupPrivs ++ globalPrivileges).toSet
      Success(allowedPrivileges.contains(tag))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Error isAuthorized user [$userId] info: ${e.getMessage}", e)
        Failure(e)
    }
  }

  override def removeAuthorization(userId: String, userType: UserType, resources: List[String]): Try[Unit] = {
    try {
      privDao.removePrivileges(userId, userType, resources)
      removeCache(userId, userType)
      Success()
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }

  override def addAuthorization(userId: String, userType: UserType, resources: List[String]): Try[Unit] = {
    try {
      privDao.addPrivileges(userId, userType, resources)
      removeCache(userId, userType)
      Success()
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }
}
