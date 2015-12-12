package com.flipkart.connekt.receptors.routes.callbacks

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
 * @version 12/8/15
 */
class Callback(implicit am: ActorMaterializer) extends BaseHandler {

  val callback =
    pathPrefix("v1") {
      authenticate {
        user =>
        path(Segment / "callback" / Segment / Segment / Segment) {
          (channel: String, appPlatform: String, app: String, devId: String) =>
            post {
              entity(as[CallbackEvent]) { e =>
                val event = e.asInstanceOf[PNCallbackEvent].copy(platform = appPlatform, appName = app, deviceId = devId)
                ServiceFactory.getCallbackService.persistCallbackEvent(event.messageId, event.deviceId, channel, event) match {
                  case Success(requestId) =>
                    ConnektLogger(LogFile.SERVICE).debug(s"Received callback event ${event.toString}")

                    complete(respond[GenericResponse](
                      StatusCodes.Created, Seq.empty[HttpHeader],
                      GenericResponse(StatusCodes.OK.intValue, null, Response("PN callback saved successfully.", null))
                    ))
                  case Failure(t) =>
                    ConnektLogger(LogFile.SERVICE).debug(s"Saving callback event failed ${event.toString} ${t.getMessage}")

                    complete(respond[GenericResponse](
                      StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                      GenericResponse(StatusCodes.OK.intValue, null, Response(s"Saving PN callback failed: ${t.getMessage}", null))
                    ))
                }
              }
            }
        }
      }
    }
}
