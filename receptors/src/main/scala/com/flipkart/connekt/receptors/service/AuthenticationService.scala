package com.flipkart.connekt.receptors.service

import com.flipkart.connekt.commons.cache.{DistributedCacheType, DistributedCacheManager}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.utils.LdapUtils

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
object AuthenticationService {

  def authenticateKey(apiKey: String): Option[AppUser] = {
    //if transient token present
    DistributedCacheManager.getCache[String](DistributedCacheType.TransientToken).get(apiKey) match {
      case Some(userId) =>
        DaoFactory.getUserInfoDao.getUserInfo(userId)
      case None =>
        //TODO: Use cache
        DaoFactory.getUserInfoDao.getUserByKey(apiKey)
    }
  }

  def authenticateLdap(username: String, password: String): Boolean = {
    LdapUtils.authenticate(username, password)
  }
}
