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
package com.flipkart.connekt.receptors.routes.common

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.{OAuthToken, SimpleCredential}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.service.{AuthenticationService, TokenService}
import com.flipkart.connekt.receptors.wire.ResponseUtils._


class UserAuthRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    pathPrefix("v1") {
      pathPrefix("auth") {
        path("ldap") {
          post {
            meteredResource("ldapAuthenticate") {
              entity(as[SimpleCredential]) {
                user =>
                  AuthenticationService.authenticateLdap(user.username, user.password) match {
                    case true =>
                      TokenService.set(user.username).get match {
                        case Some(tokenId) =>
                          complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Logged in successfully. Please note your tokenId.", Map("tokenId" -> tokenId))))
                        case None =>
                          complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Unable to generate token", null)))
                      }
                    case false =>
                      complete(GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("Unauthorised, Invalid Username/Password", null)))
                  }
              }
            }
          }
        } ~ path("gauth") {
          post {
            entity(as[OAuthToken]) {
              oAuthToken =>
                AuthenticationService.authenticateGoogleOAuth(oAuthToken.token) match {
                  case Some(user) =>
                    TokenService.set(user.userId).get match {
                      case Some(tokenId) =>
                        complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Logged in successfully. Please note your tokenId.", Map("tokenId" -> tokenId, "userId" -> user.userId))))
                      case None =>
                        complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Unable to generate token", null)))
                    }
                  case None =>
                    complete(GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("Unauthorised OR Invalid token", null)))
                }
            }
          }
        }
      }
    }
}
