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
class RegistrationRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler {

  type Created = Boolean
  type Updated = Boolean

  val register =
    pathPrefix("v1") {
      pathPrefix("registration" / "push") {
        path(MPlatformSegment / Segment / Segment) {
          (platform: MobilePlatform, appName: String, deviceId: String) =>
            put {
              authorize(user, "REGISTRATION") {
                entity(as[DeviceDetails]) { d =>
                  val newDeviceDetails = d.copy(appName = appName, osName = platform.toString, deviceId = deviceId)

                  val result = DeviceDetailsService.get(appName, deviceId).transform[Either[Updated, Created]]({
                    case Some(deviceDetail) => DeviceDetailsService.update(deviceId, newDeviceDetails).map(u => Left(true))
                    case None => DeviceDetailsService.add(newDeviceDetails).map(c => Right(true))
                  }, Failure(_)).get

                  result match {
                    case result.isRight =>
                      complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"DeviceDetails created for ${newDeviceDetails.deviceId}", newDeviceDetails)))
                    case result.isLeft =>
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails updated for ${newDeviceDetails.deviceId}", newDeviceDetails)))
                  }
                }
              }
            }
        } ~ path(Segment / "users" / Segment) {
          (appName: String, userId: String) =>
            get {
              authorize(user, "REGISTRATION_READ", s"REGISTRATION_READ_$appName") {
                val deviceDetails = DeviceDetailsService.getByUserId(appName, userId).get
                complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails fetched for app: $appName, user: $userId", Map[String, Any]("deviceDetails" -> deviceDetails))))
              }
            }
        } ~ path(MPlatformSegment / Segment / Segment) {
          (platform: MobilePlatform, appName: String, deviceId: String) =>
            get {
              authorize(user, "REGISTRATION_READ", s"REGISTRATION_READ_$appName") {
                DeviceDetailsService.get(appName, deviceId).get match {
                  case Some(deviceDetail) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails fetched for app: $appName id: $deviceId", Map[String, Any]("deviceDetails" -> deviceDetail.get))))
                  case None =>
                    complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"No DeviceDetails found for app: $appName id: $deviceId", null)))
                }
              }
            }
        }

      }
    }
}
