package com.flipkart.connekt.receptors.routes.callbacks

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}
import com.flipkart.connekt.commons.entities.Channel

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
class CallbackRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val callback =
    pathPrefix("v1") {
      path(Segment / "callback" / MPlatformSegment / Segment / Segment) {
        (channel: String, appPlatform: MobilePlatform, app: String, devId: String) =>
          post {
            entity(as[CallbackEvent]) { e =>
              val event = e.asInstanceOf[PNCallbackEvent].copy(platform = appPlatform.toString, appName = app.toLowerCase, deviceId = devId)
              ServiceFactory.getCallbackService.persistCallbackEvent(event.messageId, s"${event.appName}${event.deviceId}", Channel.PUSH, event).get
              ConnektLogger(LogFile.SERVICE).debug(s"Received callback event ${event.toString}")
              complete(GenericResponse(StatusCodes.OK.intValue, null, Response("PN callback saved successfully.", null)))
            }
          }
      }
    }
}
