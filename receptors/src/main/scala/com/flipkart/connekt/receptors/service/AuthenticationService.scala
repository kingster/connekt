package com.flipkart.connekt.receptors.service

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.LdapService
import com.flipkart.metrics.Timed

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
object AuthenticationService extends Instrumented {

  @Timed("authenticateKey")
  def authenticateKey(apiKey: String): Option[AppUser] = {
    //if transient token present
    TokenService.get(apiKey) match {
      case Some(userId) =>
        DaoFactory.getUserInfoDao.getUserInfo(userId).orElse(Option(new AppUser(userId,apiKey,"","")))
      case None =>
        DaoFactory.getUserInfoDao.getUserByKey(apiKey)
    }
  }

  def authenticateLdap(username: String, password: String): Boolean = {
    LdapService.authenticate(username, password)
  }
}
