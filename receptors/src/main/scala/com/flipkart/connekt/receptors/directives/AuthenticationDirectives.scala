package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsRejected, CredentialsMissing}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{BasicDirectives, RouteDirectives}
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.receptors.service.AuthenticationService

/**
 *
 * @author durga.s
 * @version 11/22/15
 */
trait AuthenticationDirectives {

  private def getHeader(key: String, h: Seq[HttpHeader]): Option[String] = h.find(_.name.equalsIgnoreCase(key)).flatMap(w => Option(w.value))

  val X_API_KEY_HEADER = "x-api-key"

  def authenticate: Directive1[AppUser] = {

    BasicDirectives.extract[Seq[HttpHeader]](_.request.headers) flatMap { headers =>
      getHeader(X_API_KEY_HEADER, headers) match {
        case Some(apiKey) =>
          AuthenticationService.authenticateKey(apiKey) match {
            case Some(user) =>
              provide(user)
            case None =>
              ConnektLogger(LogFile.SERVICE).warn(s"authentication failure for apiKey: [$apiKey]")
              RouteDirectives.reject(AuthenticationFailedRejection(CredentialsRejected,null))
          }
        case None =>

          RouteDirectives.reject(AuthenticationFailedRejection(CredentialsMissing,null))
      }

    }

  }

}
