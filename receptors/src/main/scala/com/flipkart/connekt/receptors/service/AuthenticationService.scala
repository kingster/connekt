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
import java.util.UUID

import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.{ConnektConfig, UserInfoService}
import com.flipkart.metrics.Timed
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import org.jboss.aerogear.security.otp.Totp
import org.jboss.aerogear.security.otp.api.{Base32, Clock}

import scala.util.{Failure, Success, Try}

object AuthenticationService extends Instrumented {

  private lazy val userService: UserInfoService = ServiceFactory.getUserInfoService

  private final val GOOGLE_OAUTH_CLIENT_ID = ConnektConfig.getString("auth.google.clientId").get
  private lazy val verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance)
    .setAudience(util.Arrays.asList(GOOGLE_OAUTH_CLIENT_ID))
    .setIssuer("accounts.google.com")
    .build()

  @Timed("authenticateKey")
  def authenticateKey(apiKey: String): Option[AppUser] = {
    userService.getUserByKey(apiKey).orElse(getTransientUser(apiKey)).getOrElse(None)
  }

  @Timed("authenticateGoogleOAuth")
  def authenticateGoogleOAuth(token:String): Try[Option[AppUser]] = {
    Try_(verifier.verify(token)) match {
      case Success(idToken) if idToken != null =>
        ConnektLogger(LogFile.ACCESS).info("GoogleOAuth verified : " + idToken.getPayload)

        val email = idToken.getPayload.getEmail
        val domainGroup = email.split("@")(1)
        val groups = userService.getUserInfo(email).getOrElse(None).map(_.getUserGroups.+:(domainGroup)).getOrElse(List.empty).distinct.mkString(",")
        val transientUser = new AppUser(email, generateTransientApiKey, groups, email)
        setTransientUser(transientUser).map {
          case true => Option(transientUser)
          case false => throw new RuntimeException("Failed to generate / persist transient-user.")
        }

      case Success(idToken) => Success(None)
      case Failure(t) =>
        ConnektLogger(LogFile.ACCESS).error("GoogleOAuth verification failed", t)
        Failure(t)
    }
  }

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

  private def setTransientUser(user: AppUser) = Try_ {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).put[AppUser](user.apiKey, user)
  }

  private def getTransientUser(apiKey: String): Try[Option[AppUser]] = Try_ {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).get[AppUser](apiKey)
  }

  private def generateTransientApiKey = UUID.randomUUID().toString.replaceAll("-", "")

  private val otpClock =  new Clock(60)
}

