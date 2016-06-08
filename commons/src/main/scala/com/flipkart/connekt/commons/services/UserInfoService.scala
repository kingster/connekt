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
import com.flipkart.connekt.commons.dao.TUserInfo
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.utils.PasswordGenerator

import scala.util.Try

class UserInfoService(userInfoDao: TUserInfo) extends TService {

  def addUserInfo(user: AppUser): Try[Unit] = Try_ {

    user.apiKey = getUserInfo(user.userId).get.map(_.apiKey).getOrElse(PasswordGenerator.generate(48, 48, 20, 20, 8, 0))

    Option(user.groups).foreach(_.split(",").map(_.trim).find(ServiceFactory.getAuthorisationService.getGroupPrivileges(_).isEmpty).
      foreach(group => throw new RuntimeException(s"AppUser ${user.userId} is part of a non-existent group $group"))
    )

    userInfoDao.addUserInfo(user)
  }

  def getUserInfo(userId: String): Try[Option[AppUser]] = Try_ {
    val key = s"id_$userId"
    LocalCacheManager.getCache(LocalCacheType.UserInfo).get[AppUser](key) match {
      case p: Some[AppUser] => p
      case None =>
        val user = userInfoDao.getUserInfo(userId)
        user.foreach(p => LocalCacheManager.getCache(LocalCacheType.UserInfo).put[AppUser](key, p))
        user
    }
  }

  def getUserByKey(apiKey: String): Try[Option[AppUser]] = Try_ {
    val key = s"key_$apiKey"
    LocalCacheManager.getCache(LocalCacheType.UserInfo).get[AppUser](key) match {
      case p: Some[AppUser] => p
      case None =>
        val user = userInfoDao.getUserByKey(apiKey)
        user.foreach(p => LocalCacheManager.getCache(LocalCacheType.UserInfo).put[AppUser](key, p))
        user
    }
  }

  def removeUserById(userId: String): Try[Unit] = Try_ {
    LocalCacheManager.getCache(LocalCacheType.UserInfo).remove(userId)
    userInfoDao.removeUserById(userId)
  }

}
