package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
class Registration(implicit am: ActorMaterializer) extends BaseHandler {

  val register =
    pathPrefix("v1") {
      authenticate {
        user =>
          pathPrefix("registration" / "push"  ) {
            path(Segment / Segment) {
              (platform: String, appName: String) =>
                authorize(user, "REGISTRATION") {
                  post {
                    entity(as[DeviceDetails]) { d =>
                      val deviceDetails = d.copy(appName = appName, osName = platform)
                      def save = DaoFactory.getDeviceDetailsDao.add(deviceDetails)
                      async(save) {
                        case Success(t) =>
                          complete(respond[GenericResponse](
                          StatusCodes.Created, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.Created.intValue, null, Response("DeviceDetails registered for %s".format(d.deviceId), null))
                        ))
                        case Failure(e) =>
                          complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("DeviceDetails registration failed for %s".format(d.deviceId), null))
                        ))
                      }
                    }
                  }
                }
            } ~ path(Segment / Segment / Segment) {
              (platform: String, appName: String, deviceId: String) =>
              authorize(user, "REGISTRATION") {
                put {
                  entity(as[DeviceDetails]) { d =>
                    val deviceDetails = d.copy(appName = appName, osName = platform, deviceId = deviceId)
                    def save = DaoFactory.getDeviceDetailsDao.add(deviceDetails)
                    async(save) {
                      case Success(t) =>
                        complete(respond[GenericResponse](
                          StatusCodes.Created, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.Created.intValue, null, Response("DeviceDetails registered for %s".format(d.deviceId), null))
                        ))
                      case Failure(e) =>
                        complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("DeviceDetails registration failed for %s".format(d.deviceId), null))
                        ))
                    }
                  }
                }
              }
            } ~ path(Segment / Segment) {
                (appName: String, deviceId: String) =>
                  authorize(user, "REGISTRATION_READ", "REGISTRATION_READ_" + appName ) {
                    get {
                      def fetch = DaoFactory.getDeviceDetailsDao.get(appName, deviceId)
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
}
