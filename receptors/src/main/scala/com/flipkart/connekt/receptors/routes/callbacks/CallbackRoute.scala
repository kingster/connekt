/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.receptors.routes.callbacks

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.{AppUser, Channel}
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

class CallbackRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseJsonHandler {

  val callback = pathPrefix("v1") {
    pathPrefix("push") {
      path("callback" / MPlatformSegment / Segment / Segment) {
        (appPlatform: MobilePlatform, app: String, devId: String) =>
          post {
            entity(as[CallbackEvent]) { e =>
              val event = e.asInstanceOf[PNCallbackEvent].copy(platform = appPlatform.toString, appName = app, deviceId = devId)
              ServiceFactory.getCallbackService.persistCallbackEvent(event.messageId, s"${event.appName.toLowerCase}${event.deviceId}", Channel.PUSH, event).get
              ConnektLogger(LogFile.SERVICE).debug(s"Received callback event ${event.toString}")
              complete(GenericResponse(StatusCodes.OK.intValue, null, Response("PN callback saved successfully.", null)))
            }
          }
      } ~ path("callback" / Segment / Segment / Segment) {
        (appName: String, contactId: String, messageId: String) =>
          authorize(user, s"DELETE_EVENTS_$appName") {
            delete {
              ConnektLogger(LogFile.SERVICE).debug(s"Received event delete request for: ${messageId.toString}")
              val deletedEvents = ServiceFactory.getCallbackService.deleteCallBackEvent(messageId, s"${appName.toLowerCase}$contactId", Channel.PUSH)
              complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"PN callback events deleted successfully for requestId: $messageId.", deletedEvents)))
            }
          }
      }
    }
  }
}
