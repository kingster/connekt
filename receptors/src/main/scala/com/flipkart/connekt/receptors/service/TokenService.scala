package com.flipkart.connekt.receptors.service

import java.util.UUID

/**
 * Created by avinash.h on 1/27/16.
 */
object TokenService {

  def getToken(): String = {
    UUID.randomUUID().toString.replaceAll("-", "")
  }

}
