package com.flipkart.connekt.receptors.routes.callbacks

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.factories.{ServiceFactory, ConnektLogger, LogFile}
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
class Callback(implicit _am: ActorMaterializer) extends BaseHandler {
  implicit val am = _am
  val callbackService = ServiceFactory.getCallbackService

  val callback =
    sniffHeaders { headers =>
      isAuthenticated(Some(headers)) {
          path("v1" / Segment / "callback" / Segment / Segment / Segment) {
            (channel: String, appPlatform: String, app:String, devId: String) =>
              post {
                entity(as[CallbackEvent]) { e =>
                  val event = e.asInstanceOf[PNCallbackEvent].copy(platform = appPlatform, appName = app, deviceId = devId)
                  callbackService.persistCallbackEvent(event.messageId, channel, event) match {
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
                        GenericResponse(StatusCodes.OK.intValue, null, Response("Saving PN callback failed: %s".format(t.getMessage), null))
                      ))
                  }
                }
              }
          }
      }
    }
}
