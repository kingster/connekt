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
package com.flipkart.connekt.receptors.routes.reports

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.entities.Channel._
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.directives.ChannelSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

class ReportsRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    authenticate {
      user =>
        pathPrefix("v1" / "reports") {
          authorize(user, "REPORTS") {
            pathPrefix("push") {
              path(Segment / "messages" / Segment / Segment / "events") {
                (appName: String, contactId: String, messageId: String) =>
                  get {
                    val events = ServiceFactory.getCallbackService.fetchCallbackEvent(messageId, s"${appName.toLowerCase}$contactId", Channel.PUSH).get
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Events fetched for messageId: $messageId contactId: $contactId fetched.", Map(contactId -> events))))
                  }
              }
            } ~ path(ChannelSegment / "messages" / Segment / Segment / "events") {
              (channel: Channel, contactId: String, messageId: String) =>
                get {
                  val events = ServiceFactory.getCallbackService.fetchCallbackEvent(messageId, s"$contactId", channel).get
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Events fetched for messageId: $messageId contactId: $contactId fetched.", Map(contactId -> events))))
                }
            } ~ path(ChannelSegment / "messages" / Segment / "events") {
              (channel: Channel, messageId: String) =>
                get {
                  val events = ServiceFactory.getCallbackService.fetchCallbackEventByMId(messageId, Channel.PUSH).get
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Events for $messageId fetched.", events)))
                }
            } ~ path(ChannelSegment / "messages" / Segment) {
              (channel: Channel, messageId: String) =>
                get {
                  val data = ServiceFactory.getPNMessageService.getRequestInfo(messageId).get // Not sure why this is getPNMessageService
                  data match {
                    case None =>
                      complete(GenericResponse(StatusCodes.NotFound.intValue, Map("messageId" -> messageId), Response(s"No Message found for messageId $messageId.", null)))
                    case Some(x) =>
                      complete(GenericResponse(StatusCodes.OK.intValue, Map("messageId" -> messageId), Response(s"Message info fetched for messageId: $messageId.", data)))
                  }
                }
            }
          }
        }
    }
}
