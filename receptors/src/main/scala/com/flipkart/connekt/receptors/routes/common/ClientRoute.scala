package com.flipkart.connekt.receptors.routes.common

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Channel.Channel
import com.flipkart.connekt.commons.entities.{AppUser, AppUserConfiguration, Channel}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.UserConfigurationService
import com.flipkart.connekt.receptors.directives.ChannelSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

/**
 * Created by kinshuk.bairagi on 15/02/16.
 */
class ClientRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseJsonHandler {


  val route = pathPrefix("v1" / "client") {
    authorize(user, "ADMIN_CLIENT") {
      pathEndOrSingleSlash {
        post {
          entity(as[AppUserConfiguration]) { userConfig =>
            val mSvc = ServiceFactory.getPNMessageService
            val clientTopic = mSvc.getClientChannelTopic(userConfig.channel.toString, userConfig.userId)
            userConfig.queueName = clientTopic
            UserConfigurationService.add(userConfig).get
            mSvc.addClientTopic(clientTopic, mSvc.partitionEstimate(userConfig.maxRate)).get

            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Client ${userConfig.userId} has been added.", userConfig)))
          }
        }
      } ~ path(Segment) {
        (clientName: String) =>
          get {
            val data = Channel.values.flatMap(ch => UserConfigurationService.get(clientName, ch).get)
            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched ClientConfig for client: $clientName", data)))
          }
      } ~ path(Segment / ChannelSegment) {
        (clientName: String, channel: Channel) =>
          get {
            UserConfigurationService.get(clientName, channel).get match {
              case Some(data) =>
                complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched ClientConfig ", data)))
              case None =>
                complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"Fetching ClientDetails failed.", Map("clientName" -> clientName, "channel" -> channel))))
            }
          }

      }

    }
  }

}
