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
package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{BasicDirectives, RouteDirectives}
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.receptors.service.AuthenticationService

trait AuthenticationDirectives {

  private def getHeader(key: String, h: Seq[HttpHeader]): Option[String] = h.find(_.is(key)).flatMap(w => Option(w.value()))

  case class TokenAuthenticationFailedRejection(message: String) extends Rejection

  val X_API_KEY_HEADER = "x-api-key"
  val X_SECURE_CODE_HEADER = "x-secure-code"

  def authenticate: Directive1[AppUser] = {
    BasicDirectives.extract[Seq[HttpHeader]](_.request.headers) flatMap { headers =>
      getHeader(X_API_KEY_HEADER, headers) match {
        case Some(apiKey) =>
          AuthenticationService.authenticateKey(apiKey) match {
            case Some(user) =>
              provide(user)
            case None =>
              ConnektLogger(LogFile.SERVICE).warn(s"authentication failure for apiKey: [$apiKey]")
              RouteDirectives.reject(AuthenticationFailedRejection(CredentialsRejected, null))
          }
        case None =>
          RouteDirectives.reject(AuthenticationFailedRejection(CredentialsMissing, null))
      }
    }
  }

  def verifySecureCode(secretFragments: String*): Directive0 = {
    BasicDirectives.pass
    /**
     * Disabling this for now. We will enable this once we discuss this org-wide for other registration api's also.
     * Pending on EWS Session.
     *
    BasicDirectives.extract[Seq[HttpHeader]](_.request.headers) flatMap { headers =>
      getHeader(X_SECURE_CODE_HEADER, headers) match {
        case Some(token) =>
          if (AuthenticationService.authenticateSecureCode(secretFragments.mkString(":"), token))
            BasicDirectives.pass
          else
            RouteDirectives.reject(TokenAuthenticationFailedRejection("Invalid Secure Code"))
        case None =>
          RouteDirectives.reject(TokenAuthenticationFailedRejection("Secure Code Missing"))
      }
    }
    */
  }

}
