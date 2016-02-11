package com.flipkart.connekt.receptors.routes.common

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Credentials
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseHandler
import com.flipkart.connekt.receptors.service.{AuthenticationService, TokenService}

import scala.collection.immutable.Seq

/**
 * Created by avinash.h on 1/21/16.
 */

class LdapAuthentication(implicit am: ActorMaterializer) extends BaseHandler {

  val route =
    pathPrefix("v1") {
      path("auth" / "ldap") {
        post {
          entity(as[Credentials]) {
            user =>
              AuthenticationService.authenticateLdap(user.username, user.password) match {
                case true =>
                  TokenService.set(user.username) match {
                    case Some(tokenId) =>
                      complete(respond[GenericResponse](
                        StatusCodes.OK, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.OK.intValue, null, Response("Logged in successfully. Please note your tokenId.", Map("tokenId" -> tokenId)))
                      ))
                    case None =>
                      complete(respond[GenericResponse](
                        StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Unable to generate token", null))
                      ))
                  }
                case false =>
                  complete(respond[GenericResponse](
                    StatusCodes.Unauthorized, Seq.empty[HttpHeader],
                    GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("Unauthorised, Invalid Username/Password", null))
                  ))
              }

          }
        }
      }
    }
}
