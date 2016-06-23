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
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

class CallbackRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    authenticate {
      user =>
        pathPrefix("v1") {
          pathPrefix("push") {
            path("callback" / MPlatformSegment / Segment / Segment) {
              (appPlatform: MobilePlatform, appName: String, deviceId: String) =>
                meteredResource(s"saveEvents.$appPlatform.$appName") {
                  verifySecureCode(appName.toLowerCase, user.apiKey, deviceId) {
                    authorize(user, "ADD_EVENTS", s"ADD_EVENTS_$appName") {
                      post {
                        entity(as[PNCallbackEvent]) { e =>
                          val event = e.copy(platform = appPlatform.toString, appName = appName, deviceId = deviceId, messageId = Option(e.messageId).orEmpty, eventType = e.eventType.toLowerCase)
                          event.validate()
                          event.persist
                          ServiceFactory.getReportingService.recordPushStatsDelta(user.userId, Some(event.contextId), None, Some(event.platform), event.appName, event.eventType)
                          ConnektLogger(LogFile.SERVICE).debug(s"Received callback event ${event.toString}")
                          complete(GenericResponse(StatusCodes.OK.intValue, null, Response("PN callback saved successfully.", null)))
                        }
                      }
                    }
                  }
                }
            } ~ path("callback" / MPlatformSegment / Segment / Segment / Segment) {
              (appPlatform: MobilePlatform, appName: String, contactId: String, messageId: String) =>
                meteredResource(s"deleteEvents.$appPlatform.$appName") {
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
    }
}
