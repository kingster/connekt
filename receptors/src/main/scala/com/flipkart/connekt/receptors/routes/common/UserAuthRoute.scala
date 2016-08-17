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
import com.flipkart.connekt.commons.entities.OAuthToken
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.service.AuthenticationService
import com.flipkart.connekt.receptors.wire.ResponseUtils._

import scala.util.{Failure, Success}


class UserAuthRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    pathPrefix("v1") {
      pathPrefix("auth") {
        path("gauth") {
          post {
            entity(as[OAuthToken]) {
              oAuthToken =>
                AuthenticationService.authenticateGoogleOAuth(oAuthToken.token) match {
                  case Success(appUser) if appUser.isDefined =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Logged in successfully. Please note your tokenId.", Map("tokenId" -> appUser.get.apiKey, "userId" -> appUser.get.userId))))
                  case Success(appUser) if appUser.isEmpty =>
                    complete(GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("Invalid google credentials / dis-allowed domain, authentication failed", null)))
                  case Failure(t) =>
                    complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Unable to generate token", null)))
                }
            }
          }
        }
      }
    }
}
