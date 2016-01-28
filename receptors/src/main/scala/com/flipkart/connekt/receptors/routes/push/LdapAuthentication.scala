package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseHandler
import com.flipkart.connekt.receptors.service.{AuthenticationService, TokenService}

import scala.collection.immutable.Seq

/**
 * Created by avinash.h on 1/21/16.
 */

case class ldapuser(username: String, password: String)

class LdapAuthentication(implicit am: ActorMaterializer) extends BaseHandler {

  val token =
    pathPrefix("v1") {
      path("auth" / "ldap") {
        post {
          entity(as[ldapuser]) { user =>
            AuthenticationService.authenticateLdap(user.username, user.password) match {
              case true =>
                val tokenId = TokenService.getToken()
                DistributedCacheManager.getCache[String](DistributedCacheType.TransientToken).put(tokenId, user.username) match {
                  case true =>
                    complete(respond[GenericResponse] (
                    StatusCodes.OK, Seq.empty[HttpHeader],
                    GenericResponse (StatusCodes.OK.intValue, null, Response ("generated token successfully", tokenId) )
                    ))
                  case false =>
                    complete(respond[GenericResponse] (
                    StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                    GenericResponse (StatusCodes.InternalServerError.intValue, null, Response ("Unable to set token in cache", null) )
                    ))
                }
              case false =>
                complete(respond[GenericResponse](
                          StatusCodes.Unauthorized, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("Unauthorised, not able to generate token",null))
                        ))
            }

          }
        }
      }
    }
}
