/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.receptors.service

import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.UserInfoService
import com.flipkart.connekt.commons.utils.LdapService
import com.flipkart.metrics.Timed
import org.jboss.aerogear.security.otp.Totp
import org.jboss.aerogear.security.otp.api.{Base32, Clock}

object AuthenticationService extends Instrumented {

  lazy val userService: UserInfoService = ServiceFactory.getUserInfoService

  @Timed("authenticateKey")
  def authenticateKey(apiKey: String): Option[AppUser] = {
    //API Key test first, since that's local, hence faster
    userService.getUserByKey(apiKey).orElse {
      //else transient token if present
      TokenService.get(apiKey).map {
        case Some(userId) => userService.getUserInfo(userId).get.orElse {
          //Now the user may not exist in our db, so that person should have access to global permissions only, so return a simple user.
          Option(new AppUser(userId, apiKey, "", s"$userId@flipkart.com"))
        }
        case None => None
      }
    }.get
  }

  @Timed("authenticateLdap")
  def authenticateLdap(username: String, password: String): Boolean = {
    LdapService.authenticate(username, password)
  }

  private val otpClock =  new Clock(60)

  @Timed("authenticateSecureCode")
  def authenticateSecureCode(secret:String, token:String): Boolean = {
    val totp = new Totp(Base32.encode(secret.getBytes), otpClock)
    totp.verify(token)
  }

  @Timed("generateSecureCode")
  def generateSecureCode(secret:String):String = {
    val totp = new Totp(Base32.encode(secret.getBytes),otpClock)
    totp.now()
  }

}
