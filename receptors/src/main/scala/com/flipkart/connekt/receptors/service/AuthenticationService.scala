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

import java.util

import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.UserInfoService
import com.flipkart.connekt.commons.utils.LdapService
import com.flipkart.metrics.Timed
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import org.jboss.aerogear.security.otp.Totp
import org.jboss.aerogear.security.otp.api.{Base32, Clock}

import scala.util.Success

object AuthenticationService extends Instrumented {

  lazy val userService: UserInfoService = ServiceFactory.getUserInfoService
  private final val GOOGLE_OAUTH_CLIENT_ID = "738499543082-dbjefe6ki5rni6088l4f5kourcn051dh.apps.googleusercontent.com"

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

  lazy val verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance)
    .setAudience(util.Arrays.asList(GOOGLE_OAUTH_CLIENT_ID)).setIssuer("accounts.google.com").build()

  @Timed("authenticateGoogleOAuth")
  def authenticateGoogleOAuth(token:String):Option[AppUser] = {
    Try_(verifier.verify(token)) match {
      case Success(idToken) if idToken != null =>
        ConnektLogger(LogFile.ACCESS).info("GoogleOAuth Verified : " + idToken.getPayload)
        val email = idToken.getPayload.getEmail
        userService.getUserInfo(email).get
      case _ => None
    }
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

