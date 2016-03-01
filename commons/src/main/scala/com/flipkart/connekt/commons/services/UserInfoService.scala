package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.TUserInfo
import com.flipkart.connekt.commons.entities.AppUser

import scala.util.Try

class UserInfoService( userInfoDao: TUserInfo) extends TService {

  def addUserInfo(user: AppUser):Try[Unit] = Try_ {
    userInfoDao.addUserInfo(user)
  }

  def getUserInfo(userId: String): Try[Option[AppUser]]  = Try_ {
    val key = s"id_$userId"
    LocalCacheManager.getCache(LocalCacheType.UserInfo).get[AppUser](key) match {
      case p: Some[AppUser] => p
      case None =>
        val user = userInfoDao.getUserInfo(userId)
        user.foreach( p => LocalCacheManager.getCache(LocalCacheType.UserInfo).put[AppUser](key, p))
        user
    }
  }

  def getUserByKey(apiKey: String): Try[ Option[AppUser]] = Try_ {
    val key = s"key_$apiKey"
    LocalCacheManager.getCache(LocalCacheType.UserInfo).get[AppUser](key) match {
      case p: Some[AppUser] => p
      case None =>
        val user = userInfoDao.getUserByKey(apiKey)
        user.foreach( p => LocalCacheManager.getCache(LocalCacheType.UserInfo).put[AppUser](key, p))
        user
    }
  }
}
