package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
class Unicast(implicit am: ActorMaterializer) extends BaseHandler {

  val unicast =
    pathPrefix("v1") {
      authenticate { user =>
        path("push" / "unicast" / Segment / Segment / Segment) {
          (appPlatform: String, appName: String, deviceId: String) =>
            authorize(user, "UNICAST_" + appName) {
              post {
                entity(as[ConnektRequest]) { r =>
                  val pnRequestInfo = r.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName, platform = appPlatform, deviceId = deviceId)
                  val unicastRequest = r.copy(channelInfo = pnRequestInfo)

                  ConnektLogger(LogFile.SERVICE).debug(s"Received unicast PN request with payload: ${r.toString}")
                  def enqueue = ServiceFactory.getMessageService.persistRequest(unicastRequest, "fk-connekt-pn", isCrucial = true)
                  async(enqueue) {
                    case Success(t) => t match {
                      case Success(requestId) =>
                        complete(respond[GenericResponse](
                            StatusCodes.Created, Seq.empty[HttpHeader],
                            GenericResponse(StatusCodes.OK.intValue, null, Response("PN Request en-queued successfully for %s".format(requestId), null))
                          ))
                      case Failure(e) =>
                        complete(respond[GenericResponse](
                            StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                            GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("PN Request processing failed: %s".format(e.getMessage), null))
                          ))
                    }
                    case Failure(e) =>
                      complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("PN Request processing failed: %s".format(e.getMessage), null))
                        ))
                  }
                }
              }
            }
        } ~ path("push" / "openwebpn" / Segment / Segment) {
          (appName: String, deviceId: String) =>
            authorize(user, "OPENWEBPN_" + appName) {
              post {
                entity(as[ConnektRequest]) { r =>
                  val pnRequestInfo = r.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName, deviceId = deviceId)
                  val unicastRequest = r.copy(channelInfo = pnRequestInfo)

                  ConnektLogger(LogFile.SERVICE).debug(s"Received openwebPN request with payload: ${r.toString}")
                  def enqueue = ServiceFactory.getMessageService.saveFetchRequest(unicastRequest, isCrucial = true)
                  async(enqueue) {
                    case Success(t) => t match {
                      case Success(requestId) =>
                        complete(respond[GenericResponse](
                          StatusCodes.Created, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.OK.intValue, null, Response("openwebPN Request en-queued successfully for %s".format(requestId), null))
                        ))
                      case Failure(e) =>
                        complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("openwebPN Request processing failed: %s".format(e.getMessage), null))
                        ))
                    }
                    case Failure(e) =>
                      complete(respond[GenericResponse](
                        StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("openwebPN Request processing failed: %s".format(e.getMessage), null))
                      ))
                  }
                }
              }
            }
        }
      }
    }
}
