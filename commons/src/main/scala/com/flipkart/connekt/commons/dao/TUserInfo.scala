/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.AppUser

trait TUserInfo {
  def getUserInfo(userId: String): Option[AppUser]
  def addUserInfo(user: AppUser)
  def getUserByKey(key: String): Option[AppUser]
}
