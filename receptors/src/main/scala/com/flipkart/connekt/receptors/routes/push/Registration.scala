package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.entities.{AppUser, DeviceDetails}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
class Registration(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler {

  val register =
    pathPrefix("v1") {
          pathPrefix("registration" / "push") {
            path(MPlatformSegment / Segment / Segment) {
              (platform: MobilePlatform, appName: String, deviceId: String) =>
                put {
                  authorize(user, "REGISTRATION") {
                    entity(as[DeviceDetails]) { d =>
                      val deviceDetails = d.copy(appName = appName, osName = platform.toString, deviceId = deviceId)
                      DeviceDetailsService.get(appName, deviceId) match {
                        case None =>
                          DeviceDetailsService.add(deviceDetails)
                          complete(respond[GenericResponse](
                            StatusCodes.Created, Seq.empty[HttpHeader],
                            GenericResponse(StatusCodes.Created.intValue, null, Response("DeviceDetails created for %s".format(deviceDetails.deviceId), null))
                          ))
                        case Some(existingDevice) =>
                          DeviceDetailsService.update(deviceId, deviceDetails)
                          complete(respond[GenericResponse](
                            StatusCodes.OK, Seq.empty[HttpHeader],
                            GenericResponse(StatusCodes.OK.intValue, null, Response("DeviceDetails updated for %s".format(deviceDetails.deviceId), null))
                          ))
                      }
                      
                    }
                  }
                }
            } ~ path(Segment / "users" / Segment) {
              (appName: String, userId: String) =>
                get {
                  authorize(user, "REGISTRATION_READ", "REGISTRATION_READ_" + appName) {
                    def fetch = DeviceDetailsService.getByUserId(appName, userId)
                    async(fetch) {
                      case Success(deviceDetails) =>
                        complete(respond[GenericResponse](
                          StatusCodes.OK, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.OK.intValue, null, Response("DeviceDetails fetched for app: %s user: %s".format(appName, userId), Map[String, Any]("deviceDetails" -> deviceDetails)))
                        ))
                      case Failure(error) =>
                        complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Fetching DeviceDetails failed for app:%s %s".format(appName, userId), null))
                        ))
                    }
                  }
                }
            } ~ path(MPlatformSegment / Segment / Segment) {
              (platform: MobilePlatform, appName: String, deviceId: String) =>
                get {
                  authorize(user, "REGISTRATION_READ", s"REGISTRATION_READ_$appName") {
                    def fetch = DeviceDetailsService.get(appName, deviceId)
                    async(fetch) {
                      case Success(resultOption) =>
                        resultOption match {
                          case Some(deviceDetails) =>
                            complete(respond[GenericResponse](
                              StatusCodes.OK, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.OK.intValue, null, Response("DeviceDetails fetched for app: %s %s".format(appName, deviceId), Map[String, Any]("deviceDetails" -> deviceDetails)))
                            ))
                          case None =>
                            complete(respond[GenericResponse](
                              StatusCodes.OK, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.OK.intValue, null, Response("No DeviceDetails found for app:%s %s".format(appName, deviceId), null))
                            ))
                        }
                      case Failure(error) =>
                        complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Fetching DeviceDetails failed for app:%s %s".format(appName, deviceId), null))
                        ))
                    }
                  }
                }
            }

          }
    }
}
