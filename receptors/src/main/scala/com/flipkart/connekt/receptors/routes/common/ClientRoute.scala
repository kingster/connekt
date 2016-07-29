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
import com.flipkart.connekt.commons.entities.UserType.UserType
import com.flipkart.connekt.commons.entities.{AppUser, AppUserConfiguration, Channel, UserType, _}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger, ServiceFactory}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.UserConfigurationService
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.receptors.directives.UserTypeSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

class ClientRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    authenticate {
      user =>
        pathPrefix("v1") {
          pathPrefix("client") {
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
                      au.validate()
                      ServiceFactory.getUserInfoService.addUserInfo(au).get
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Client ${au.userId} has been added.", au)))
                    }
                  }
                } ~ path(Segment / "configuration") {
                  (clientName: String) =>
                    put {
                      entity(as[AppUserConfiguration]) { userConfig =>
                        userConfig.userId = clientName
                        userConfig.validate()

                        ServiceFactory.getUserInfoService.getUserInfo(clientName).get match {
                          case None =>
                            complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"User $clientName: does not exist.", null)))
                          case Some(userInfo) =>
                            val mSvc = ServiceFactory.getPNMessageService

                            UserConfigurationService.get(userConfig.userId, userConfig.channel).get match {
                              case Some(uConfig) =>
                                uConfig.platforms = userConfig.platforms
                                uConfig.maxRate = userConfig.maxRate
                                UserConfigurationService.add(uConfig).get
                                userConfig.queueName = uConfig.queueName
                                ConnektLogger(LogFile.SERVICE).info(s"AppUserConfig updated for user ${userConfig.userId}")

                              case None =>
                                val clientTopic = mSvc.assignClientChannelTopic(userConfig.channel, userConfig.userId)
                                userConfig.queueName = clientTopic
                                UserConfigurationService.add(userConfig).get
                                mSvc.addClientTopic(clientTopic, mSvc.partitionEstimate(userConfig.maxRate)).get
                                SyncManager.get().publish(SyncMessage(topic = SyncType.CLIENT_QUEUE_CREATE, List(clientName,clientTopic)))
                                ConnektLogger(LogFile.SERVICE).info(s"AppUserConfig added for user ${userConfig.userId}")
                            }

                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Client ${userConfig.userId} has been added/updated.", userConfig)))
                        }
                      }
                    }
                }
              } ~ path("touch" / Segment) {
                (clientName: String) =>
                  post {
                    SyncManager.get().publish(new SyncMessage(SyncType.AUTH_CHANGE, List(clientName, UserType.USER.toString)))
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Triggered  Change for client: $clientName", null)))
                  }
              } ~ pathPrefix("grant") {
                authorize(user, "ADMIN_CLIENT") {
                  path(UserTypeSegment / Segment) {
                    (userType: UserType, id: String) =>
                      post {
                        entity(as[ResourcePriv]) { resourcePriv =>
                          val resourceList = resourcePriv.resources.split(",").map(_.trim).map(_.toUpperCase).toList
                          userType match {
                            case UserType.USER =>
                              ServiceFactory.getUserInfoService.getUserInfo(id).get match {
                                case None =>
                                  complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"User $id: does not exist.", null)))
                                case Some(userInfo) =>
                                  ServiceFactory.getAuthorisationService.addAuthorization(id, UserType.USER, resourceList)
                                  complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Permission granted for $id.", Map("user" -> id, "permissions" -> resourceList))))
                              }
                            case _ =>
                              val resourceList = resourcePriv.resources.split(",").map(_.trim).map(_.toUpperCase).toList
                              ServiceFactory.getAuthorisationService.addAuthorization(id, userType, resourceList)
                              complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Permission granted for $id", Map("id" -> id, "permissions" -> resourceList))))
                          }
                        }
                      }
                  }
                }
              } ~ pathPrefix("revoke") {
                authorize(user, "ADMIN_CLIENT") {
                  path(UserTypeSegment / Segment) {
                    (userType: UserType, id: String) =>
                      post {
                        entity(as[ResourcePriv]) { resourcePriv =>
                          val resourceList = resourcePriv.resources.split(",").map(_.trim).map(_.toUpperCase).toList
                          userType match {
                            case UserType.USER =>
                              ServiceFactory.getUserInfoService.getUserInfo(id).get match {
                                case None =>
                                  complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"User $id: does not exist.", null)))
                                case Some(userInfo) =>
                                  ServiceFactory.getAuthorisationService.removeAuthorization(id, UserType.USER, resourceList)
                                  complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Permission revoked for $id.", Map("user" -> id, "permissions" -> resourceList))))
                              }
                            case _ =>
                              val resourceList = resourcePriv.resources.split(",").map(_.trim).map(_.toUpperCase).toList
                              ServiceFactory.getAuthorisationService.removeAuthorization(id, userType, resourceList)
                              complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Permission revoked for $id", Map("id" -> id, "permissions" -> resourceList))))
                          }
                        }
                      }
                  }
                }
              }
            }
          }
        }
    }
}
