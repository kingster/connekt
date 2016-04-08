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
import com.flipkart.connekt.commons.iomodels.{GenericResponse, PNCallbackEvent, Response}
import com.flipkart.connekt.receptors.directives.ChannelSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

class ReportsRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    authenticate {
      user =>
        pathPrefix("v1" / "reports") {
          authorize(user, "REPORTS") {
            pathPrefix("push") {
              path(Segment / "messages" / Segment / Segment / "events") {
                (appName: String, deviceId: String, messageId: String) =>
                  get {
                    val events = ServiceFactory.getCallbackService.fetchCallbackEvent(messageId, s"${appName.toLowerCase}$deviceId", Channel.PUSH).get
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Events fetched for messageId: $messageId contactId: $deviceId fetched.", Map(deviceId -> events.map(_._1)))))
                  }
              } ~ path(Segment / "messages" / Segment) {
                (appName: String, contactId: String) =>
                  get {
                    parameters("startTs" ? 0L) { (startTs) =>
                      val events = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(s"${appName.toLowerCase}$contactId", Channel.PUSH, startTs, System.currentTimeMillis()).getOrElse(Nil)
                      val messages = events.map(e => ServiceFactory.getPNMessageService.getRequestInfo(e._1.asInstanceOf[PNCallbackEvent].messageId).getOrElse(None).orNull)
                      val finalTs = events.map(_._2).reduceLeftOption(_ max _).getOrElse(System.currentTimeMillis)

                      complete(GenericResponse(StatusCodes.OK.intValue, Map("contactId" -> contactId, "appName" -> appName, "startTs" -> startTs ), Response(s"messages fetched for $appName / $contactId", Map("messages" -> messages, "endTs" -> finalTs, "count" -> messages.size))))
                    }
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
