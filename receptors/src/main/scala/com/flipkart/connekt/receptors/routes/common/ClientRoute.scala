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
import com.flipkart.connekt.commons.entities.{AppUser, AppUserConfiguration, Channel, UserType}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.UserConfigurationService
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

class ClientRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseJsonHandler {

  val route = pathPrefix("v1" / "client") {
    authorize(user, "ADMIN_CLIENT") {
      path(Segment) {
        (clientName: String) =>
          get {
            ServiceFactory.getUserInfoService.getUserInfo(clientName).get match {
              case Some(data) =>
                complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Client $clientName's api-key fetched.", data)))
              case None =>
                complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"Fetching client $clientName info failed.", null)))
            }
          }
      } ~ path(Segment / "config") {
        (clientName: String) =>
          get {
            val data = Channel.values.flatMap(ch => UserConfigurationService.get(clientName, ch).get)
            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched ClientConfig for client: $clientName", data)))
          }
      } ~ pathPrefix("create") {
        pathEndOrSingleSlash {
          post {
            entity(as[AppUser]) { au =>
              au.updatedBy = user.userId
              ServiceFactory.getUserInfoService.addUserInfo(au).get
              complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Client ${au.userId} has been added.", au)))
            }
          }
        } ~ path(Segment / "configuration") {
          (clientName: String) =>
            post {
              entity(as[AppUserConfiguration]) { userConfig =>
                val mSvc = ServiceFactory.getPNMessageService
                val clientTopic = mSvc.assignClientChannelTopic(userConfig.channel, clientName)
                userConfig.userId = clientName
                userConfig.queueName = clientTopic
                UserConfigurationService.add(userConfig).get
                mSvc.addClientTopic(clientTopic, mSvc.partitionEstimate(userConfig.maxRate)).get
                complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Client $clientName has been added.", userConfig)))
              }
            }
        }
      } ~ path("touch" / Segment) {
        (clientName: String) =>
          post {
            SyncManager.get().publish(new SyncMessage(SyncType.AUTH_CHANGE, List(clientName, UserType.USER.toString)))
            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Triggered  Change for client: $clientName", null)))
          }
      }
    }
  }

}
