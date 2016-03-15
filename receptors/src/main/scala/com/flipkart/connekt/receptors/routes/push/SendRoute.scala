package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.entities.{AppUser, MobilePlatform}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
class SendRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseJsonHandler {

  val route =
    pathPrefix("v1") {
      pathPrefix("send" / "push") {
        path(MPlatformSegment / Segment) {
          (appPlatform: MobilePlatform, appName: String) =>
            authorize(user, "SEND_" + appName) {
              post {
                getXHeaders { headers =>
                  entity(as[ConnektRequest]) { r =>
                    val request = r.copy(channel = "push", meta = headers)
                    ConnektLogger(LogFile.SERVICE).debug(s"Received PN request with payload: ${request.toString}")

                    /* Find platform for each deviceId, group */
                    if (request.validate()) {
                      val pnRequestInfo = request.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName.toLowerCase)
                      val groupedPlatformRequests = ListBuffer[ConnektRequest]()

                      appPlatform match {
                        case MobilePlatform.UNKNOWN =>
                          val groupedDevices = DeviceDetailsService.get(pnRequestInfo.appName, pnRequestInfo.deviceId).get.groupBy(_.osName).mapValues(_.map(_.deviceId))
                          groupedPlatformRequests ++= groupedDevices.map { case (platform, deviceId) =>
                            platform -> request.copy(channelInfo = pnRequestInfo.copy(platform = platform, deviceId = deviceId))
                          }.values
                        case _ =>
                          groupedPlatformRequests += request.copy(channelInfo = pnRequestInfo.copy(platform = appPlatform))
                      }

                      val failure = ListBuffer[String]()
                      val success = scala.collection.mutable.Map[String, List[String]]()

                      val queueName = ServiceFactory.getPNMessageService.getRequestBucket(request, user)

                      groupedPlatformRequests.foreach { p =>
                        /* enqueue multiple requests into kafka */
                        ServiceFactory.getPNMessageService.saveRequest(p, queueName, isCrucial = true) match {
                          case Success(id) =>
                            success += id -> p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                          case Failure(t) =>
                            failure ++= p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                        }
                      }
                      complete(GenericResponse(StatusCodes.Created.intValue, null, SendResponse("PN request processed.", success.toMap, failure.toList)))
                    } else {
                      ConnektLogger(LogFile.SERVICE).error(s"Invalid templateId or Channel Request data for ${r.templateId} ")
                      complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Invalid request. templateId/ChannelRequestData not valid", null)))
                    }
                  }
                }
              }
            }
        } ~ path(MPlatformSegment / Segment / "users" / Segment) {
          (appPlatform: MobilePlatform, appName: String, userId: String) =>
            authorize(user, "SEND_" + appName) {
              post {
                getXHeaders { headers =>
                  entity(as[ConnektRequest]) { r =>
                    val request = r.copy(channel = "push")
                    ConnektLogger(LogFile.SERVICE).debug(s"Received PN request sent for user : $userId with payload: ${request.toString}")

                    if (request.validate()) {
                      val pnRequestInfo = request.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName.toLowerCase)
                      val groupedPlatformRequests = ListBuffer[ConnektRequest]()

                      appPlatform match {
                        case MobilePlatform.UNKNOWN =>
                          val groupedDevices = DeviceDetailsService.getByUserId(appName.toLowerCase, userId).get.groupBy(_.osName).mapValues(_.map(_.deviceId))
                          groupedPlatformRequests ++= groupedDevices.map { case (platform, deviceId) =>
                            platform -> request.copy(channelInfo = pnRequestInfo.copy(platform = platform, deviceId = deviceId))
                          }.values
                        case _ =>
                          val osSpecificDeviceIds = DeviceDetailsService.getByUserId(appName.toLowerCase, userId).get.filter(_.osName == appPlatform.toLowerCase).map(_.deviceId)
                          if (osSpecificDeviceIds.nonEmpty)
                            groupedPlatformRequests += request.copy(channelInfo = pnRequestInfo.copy(platform = appPlatform, deviceId = osSpecificDeviceIds))
                      }

                      val failure = ListBuffer[String]()
                      val success = scala.collection.mutable.Map[String, List[String]]()

                      val queueName = ServiceFactory.getPNMessageService.getRequestBucket(request, user)


                      if (groupedPlatformRequests.nonEmpty) {
                        groupedPlatformRequests.foreach { p =>
                          ServiceFactory.getPNMessageService.saveRequest(p, queueName, isCrucial = true) match {
                            case Success(id) =>
                              success += id -> p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                            case Failure(t) =>
                              failure ++= p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                          }
                        }
                        complete(GenericResponse(StatusCodes.Created.intValue, null, SendResponse(s"PN request processed for user $userId.", success.toMap, failure.toList)))
                      } else {
                        complete(GenericResponse(StatusCodes.NotFound.intValue, null, SendResponse(s"No device Registred for user: $userId.", success.toMap, failure.toList)))
                      }

                    } else {
                      ConnektLogger(LogFile.SERVICE).error(s"Invalid templateId or Channel Request data for ${r.templateId} ")
                      complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Invalid request. templateId/ChannelRequestData not valid", null)))
                    }
                  }
                }
              }
            }
        }
      }
    }

}
