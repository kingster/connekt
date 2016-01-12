package com.flipkart.connekt.receptors.service

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.AppUser

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
object AuthenticationService {

  def authenticateKey(apiKey: String): Option[AppUser] = {
    //TODO: Use cache
    DaoFactory.getUserInfoDao.getUserByKey(apiKey)
  }
}
