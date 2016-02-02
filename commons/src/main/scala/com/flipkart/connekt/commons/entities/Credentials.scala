package com.flipkart.connekt.commons.entities

import org.apache.commons.lang.StringUtils

/**
 * Created by kinshuk.bairagi on 23/09/14.
 */
case class Credentials(username: String, password: String) {

  def isEmpty:Boolean = {
    true
   // Utility.isNullOrEmpty(username) && Utility.isNullOrEmpty(password)
  }

}

object Credentials {

  val EMPTY = Credentials(StringUtils.EMPTY, StringUtils.EMPTY)

}
