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

import scala.util.{Success, Try}

object AuthenticationService extends Instrumented {

  private lazy val userService: UserInfoService = ServiceFactory.getUserInfoService

  private lazy final val googlePublicCertsUri = ConnektConfig.getString("auth.google.publicCertsUri").getOrElse("https://www.googleapis.com/oauth2/v1/certs")
  private lazy final val GOOGLE_OAUTH_CLIENT_ID = ConnektConfig.getString("auth.google.clientId").get
  private lazy final val ALLOWED_DOMAINS = ConnektConfig.getList[String]("auth.google.allowedDomains")
  private lazy val verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance)
    .setAudience(util.Arrays.asList(GOOGLE_OAUTH_CLIENT_ID))
    .setIssuer("accounts.google.com")
    .setPublicCertsEncodedUrl(googlePublicCertsUri)
    .build()

  @Timed("authenticateKey")
  def authenticateKey(apiKey: String): Option[AppUser] = {
    if (apiKey != null) {
      userService.getUserByKey(apiKey) match {
        case Success(Some(user)) => Option(user)
        case _ => getTransientUser(apiKey).getOrElse(None)
      }
    } else None
  }

  @Timed("authenticateGoogleOAuth")
  def authenticateGoogleOAuth(token:String): Try[Option[AppUser]] = {
    Try_(verifier.verify(token)) map {
      case idToken if idToken != null =>
        ConnektLogger(LogFile.ACCESS).info("GoogleOAuth verified : " + idToken.getPayload)

        val email = idToken.getPayload.getEmail
        val domainGroup = email.split("@")(1)
        ALLOWED_DOMAINS.contains(domainGroup) match {
          case true =>
            val groups = (domainGroup :: userService.getUserInfo(email).getOrElse(None).map(_.getUserGroups).getOrElse(List.empty)).distinct.mkString(",")
            val transientUser = new AppUser(email, generateTransientApiKey, groups, email)
            addTransientUser(transientUser).map {
              case true => Option(transientUser)
              case false => throw new RuntimeException("Failed to generate / persist transient-user.")
            }.get

          case false => None
        }

      case idToken => None
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

  private def addTransientUser(user: AppUser) = Try_ {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).put[AppUser](user.apiKey, user)
  }

  private def getTransientUser(apiKey: String): Try[Option[AppUser]] = Try_ {
    DistributedCacheManager.getCache(DistributedCacheType.TransientUsers).get[AppUser](apiKey)
  }

  private def generateTransientApiKey = UUID.randomUUID().toString.replaceAll("-", "")

  private val otpClock =  new Clock(60)
}

