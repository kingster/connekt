package com.flipkart.connekt.receptors.service

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
object AuthenticationService {

  def authenticateKey(apiKey: String): Option[String] = {

    //TODO
    apiKey.equals("connekt-genesis") match {
      case true => Some("connekt-genesis")
      case false => None
    }

  }
}
