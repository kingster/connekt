package com.flipkart.connekt.commons.entities

import com.flipkart.connekt.commons.utils.StringUtils
import org.apache.commons.lang.{StringUtils => ApacheStringUtils}

/**
 * Created by kinshuk.bairagi on 23/09/14.
 */
case class Credentials(username: String, password: String) {

  def isEmpty:Boolean = {
    StringUtils.isNullOrEmpty(username) && StringUtils.isNullOrEmpty(password)
  }

}

object Credentials {

  val EMPTY = Credentials(ApacheStringUtils.EMPTY, ApacheStringUtils.EMPTY)

}
