package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, GenericResponse, Response}
import com.flipkart.connekt.commons.services.PNMessageService
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.Success

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
class Unicast(implicit _am: ActorMaterializer) extends BaseHandler {
  implicit val am = _am

  val unicast =
    sniffHeaders { headers =>
      isAuthenticated(Some(headers)) {
        path("v1" / "push" / "unicast" / Segment / Segment / Segment) {
          (appPlatform: String, appName:String, deviceId: String) =>
            post {
              entity(as[ConnektRequest]) { r =>
                ConnektLogger(LogFile.SERVICE).info("Received unicast PN request with payload: %s".format(r.toString))
                def enqueue = PNMessageService.persistRequest(r, isCrucial = true)
                async(enqueue) {
                  case Success(Some(t)) =>
                    complete(respond[GenericResponse](
                      StatusCodes.Created, Seq.empty[HttpHeader],
                      GenericResponse(StatusCodes.OK.intValue, null, Response("PN Request en-queued successfully for %s".format(t), null))
                    ))
                  case Success(None) =>
                    complete(respond[GenericResponse](
                      StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                      GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("PN Request processing failed", null))
                    ))
                }
              }
            }
        }
      }
    }
}
