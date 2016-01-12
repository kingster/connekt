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
          pathPrefix("push" / "device" / "registration") {
            path("save") {
              authorize(user, "REGISTRATION") {
                (post | put) {
                  entity(as[DeviceDetails]) { d =>
                    def save = DaoFactory.getDeviceDetailsDao.saveDeviceDetails(d)
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
            } ~
              path("fetch" / Segment / Segment) {
                (appName: String, deviceId: String) =>
                  authorize(user, "REGISTRATION_READ", "REGISTRATION_READ_"+appName ) {
                    get {
                      def fetch = DaoFactory.getDeviceDetailsDao.fetchDeviceDetails(appName, deviceId)
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
