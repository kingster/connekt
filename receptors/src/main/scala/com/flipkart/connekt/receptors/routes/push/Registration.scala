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
                  val newDeviceDetails = d.copy(appName = appName, osName = platform.toString, deviceId = deviceId)
                  val result = DeviceDetailsService.get(appName, deviceId).transform[Unit]({
                    case Some(deviceDetail) => DeviceDetailsService.update(deviceId, newDeviceDetails)
                    case None => DeviceDetailsService.add(newDeviceDetails)
                  }, Failure(_))

                  result match {
                    case Success(r) =>
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails updated for ${newDeviceDetails.deviceId}", newDeviceDetails)).respond)
                    case Failure(e) =>
                      complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"DeviceDetails registration failed for ${newDeviceDetails.deviceId}", newDeviceDetails)).respond)
                  }
                }
              }
            }
        } ~ path(Segment / "users" / Segment) {
          (appName: String, userId: String) =>
            get {
              authorize(user, "REGISTRATION_READ", "REGISTRATION_READ_" + appName) {
                DeviceDetailsService.getByUserId(appName, userId) match {
                  case Success(deviceDetails) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("DeviceDetails fetched for app: %s user: %s".format(appName, userId), Map[String, Any]("deviceDetails" -> deviceDetails))).respond)
                  case Failure(e) =>
                    complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Fetching DeviceDetails failed for app:%s %s".format(appName, userId), null)).respond)
                }
              }
            }
        } ~ path(MPlatformSegment / Segment / Segment) {
          (platform: MobilePlatform, appName: String, deviceId: String) =>
            get {
              authorize(user, "REGISTRATION_READ", s"REGISTRATION_READ_$appName") {
                DeviceDetailsService.get(appName, deviceId) match {
                  case Success(deviceDetail) if deviceDetail.isDefined =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails fetched for app: [$appName] id: [$deviceId]", Map[String, Any]("deviceDetails" -> deviceDetail.get))).respond)
                  case Success(deviceDetail) if deviceDetail.isEmpty =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"No DeviceDetails found for app: [$appName] id: [$deviceId]", null)).respond)
                  case Failure(e) =>
                    complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"Fetching DeviceDetails failed for app: [$appName] id: [$deviceId]", null)).respond)
                }
              }
            }
        }

      }
    }
}
