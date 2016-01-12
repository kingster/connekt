package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{BasicDirectives, RouteDirectives}
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.receptors.service.AuthenticationService

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
trait AuthenticationDirectives {

  private def getHeader(key: String, h: Seq[HttpHeader]) = h.find(_.name.equalsIgnoreCase(key)).flatMap(w => Some(w.value)).orNull

  val X_API_KEY_HEADER = "x-api-key"

  def authenticate: Directive1[AppUser] = {


    BasicDirectives.extract[Seq[HttpHeader]](_.request.headers) flatMap  { headers =>
      val apiKey = getHeader(X_API_KEY_HEADER, headers)
       AuthenticationService.authenticateKey(apiKey) match {
        case Some(user) =>
          provide(user)
        case None =>
          RouteDirectives.failWith(new Exception(s"authentication failure for apiKey: [$apiKey]"))
      }
    }

  }

}
