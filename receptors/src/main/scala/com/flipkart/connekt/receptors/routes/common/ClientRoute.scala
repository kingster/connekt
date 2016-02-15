package com.flipkart.connekt.receptors.routes.common

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.iomodels.{Response, GenericResponse}
import com.flipkart.connekt.receptors.routes.BaseHandler

/**
 * Created by kinshuk.bairagi on 15/02/16.
 */
class ClientRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler {

  val route = pathPrefix("v1" / "client") {
    authorize(user, "ADMIN_CLIENT") {
      pathEndOrSingleSlash {
        post {
          complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"Fetching ClientDetails failed for ", null)))
        }
      } ~ path(Segment) {
        (clientName: String) =>
          get {
            complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"Fetching ClientDetails failed for client: $clientName", null)))
          }
      }

    }
  }

}
