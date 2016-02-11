package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.entities.Credentials
import com.typesafe.config.{Config => TypesafeConfig}


/**
 * Created by kinshuk.bairagi on 13/11/14.
 */

object CredentialManager {

  def getCredential(propPath: String): Credentials = {
    val username = ConnektConfig.getString(propPath + ".username").orNull
    val password = ConnektConfig.getString(propPath + ".password").orNull

    if (username == null || password == null)
      Credentials.EMPTY
    else
      Credentials(username, password)
  }


}