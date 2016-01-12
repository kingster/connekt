package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.AppUser

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
trait TUserInfo {
  def getUserInfo(userId: String): Option[AppUser]
  def addUserInfo(user: AppUser)
  def getUserByKey(key: String): Option[AppUser]
}
