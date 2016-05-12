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
package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Channel.PUSH
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

class SendRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    authenticate {
      user =>
        pathPrefix("v1") {
          pathPrefix("send" / "push") {
            path(MPlatformSegment / Segment) {
              (appPlatform: MobilePlatform, appName: String) =>
                authorize(user, "SEND_" + appName) {
                  post {
                    meteredResource(s"sendDevicePush.$appPlatform.$appName") {
                      getXHeaders { headers =>
                        entity(as[ConnektRequest]) { r =>

                          val request = r.copy(channel = "push", meta = headers + ("client" -> user.userId))
                          request.validate()

                          ConnektLogger(LogFile.SERVICE).debug(s"Received PN request with payload: ${request.toString}")

                          val pnRequestInfo = request.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName.toLowerCase)

                          if (pnRequestInfo.deviceIds != null && pnRequestInfo.deviceIds.nonEmpty) {

                            val groupedPlatformRequests = ListBuffer[ConnektRequest]()

                            /* Find platform for each deviceId, group */
                            appPlatform match {
                              case MobilePlatform.UNKNOWN =>
                                val groupedDevices = DeviceDetailsService.get(pnRequestInfo.appName, pnRequestInfo.deviceIds).get.groupBy(_.osName).mapValues(_.map(_.deviceId).toSet)
                                groupedPlatformRequests ++= groupedDevices.map { case (platform, deviceId) =>
                                  request.copy(channelInfo = pnRequestInfo.copy(platform = platform, deviceIds = deviceId))
                                }
                              case _ =>
                                groupedPlatformRequests += request.copy(channelInfo = pnRequestInfo.copy(platform = appPlatform))
                            }

                            val failure = ListBuffer(pnRequestInfo.deviceIds.toList.diff(groupedPlatformRequests.flatMap(_.deviceId)): _ *)
                            val success = scala.collection.mutable.Map[String, Set[String]]()

                            if (groupedPlatformRequests.nonEmpty) {
                              val queueName = ServiceFactory.getPNMessageService.getRequestBucket(request, user)
                              groupedPlatformRequests.foreach { p =>
                                /* enqueue multiple requests into kafka */
                                ServiceFactory.getPNMessageService.saveRequest(p, queueName, isCrucial = true) match {
                                  case Success(id) =>
                                    val deviceIds = p.channelInfo.asInstanceOf[PNRequestInfo].deviceIds
                                    success += id -> deviceIds
                                    ServiceFactory.getReportingService.recordPushStatsDelta(user.userId, request.contextId, request.templateId, Option(p.platform), appName, InternalStatus.Received, deviceIds.size)
                                  case Failure(t) =>
                                    val deviceIds = p.channelInfo.asInstanceOf[PNRequestInfo].deviceIds
                                    failure ++= deviceIds
                                    ServiceFactory.getReportingService.recordPushStatsDelta(user.userId, request.contextId, request.templateId, Option(p.platform), appName, InternalStatus.Rejected, deviceIds.size)
                                }
                              }
                            }
                            complete(GenericResponse(StatusCodes.Created.intValue, null, SendResponse("PN Send Request Received", success.toMap, failure.toList)))
                          } else {
                            ConnektLogger(LogFile.SERVICE).error(s"Request Validation Failed, $request ")
                            complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Request Validation Failed, Please ensure mandatory field values.", null)))
                          }
                        }
                      }
                    }
                  }
                }
            } ~ path(MPlatformSegment / Segment / "users" / Segment) {
              (appPlatform: MobilePlatform, appName: String, userId: String) =>
                authorize(user, "SEND_" + appName) {
                  post {
                    meteredResource(s"sendUserPush.$appPlatform.$appName") {
                      getXHeaders { headers =>
                        entity(as[ConnektRequest]) { r =>

                          val request = r.copy(channel = "push", meta = Map("client" -> user.userId))
                          request.validate()

                          ConnektLogger(LogFile.SERVICE).debug(s"Received PN request sent for user : $userId with payload: ${request.toString}")

                          val pnRequestInfo = request.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName.toLowerCase)
                          val groupedPlatformRequests = ListBuffer[ConnektRequest]()

                          appPlatform match {
                            case MobilePlatform.UNKNOWN =>
                              val groupedDevices = DeviceDetailsService.getByUserId(appName.toLowerCase, userId).get.groupBy(_.osName).mapValues(_.map(_.deviceId).toSet)
                              groupedPlatformRequests ++= groupedDevices.map { case (platform, deviceId) =>
                                platform -> request.copy(channelInfo = pnRequestInfo.copy(platform = platform, deviceIds = deviceId))
                              }.values
                            case _ =>
                              val osSpecificDeviceIds = DeviceDetailsService.getByUserId(appName.toLowerCase, userId).get.filter(_.osName == appPlatform.toLowerCase).map(_.deviceId).toSet
                              if (osSpecificDeviceIds.nonEmpty)
                                groupedPlatformRequests += request.copy(channelInfo = pnRequestInfo.copy(platform = appPlatform, deviceIds = osSpecificDeviceIds))
                          }

                          val failure = ListBuffer[String]()
                          val success = scala.collection.mutable.Map[String, Set[String]]()

                          if (groupedPlatformRequests.nonEmpty) {
                            val queueName = ServiceFactory.getPNMessageService.getRequestBucket(request, user)

                            groupedPlatformRequests.foreach { p =>
                              ServiceFactory.getPNMessageService.saveRequest(p, queueName, isCrucial = true) match {
                                case Success(id) =>
                                  val deviceIds = p.channelInfo.asInstanceOf[PNRequestInfo].deviceIds
                                  success += id -> deviceIds
                                  ServiceFactory.getReportingService.recordPushStatsDelta(user.userId, request.contextId, request.templateId, Option(p.platform), appName, InternalStatus.Received, deviceIds.size)
                                case Failure(t) =>
                                  val deviceIds = p.channelInfo.asInstanceOf[PNRequestInfo].deviceIds
                                  failure ++= deviceIds
                                  ServiceFactory.getReportingService.recordPushStatsDelta(user.userId, request.contextId, request.templateId, Option(p.platform), appName, InternalStatus.Rejected, deviceIds.size)
                              }
                            }

                            complete(GenericResponse(StatusCodes.Created.intValue, null, SendResponse(s"PN request processed for user $userId.", success.toMap, failure.toList)))
                          } else {
                            complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"No device Found for user: $userId.", null)))
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
