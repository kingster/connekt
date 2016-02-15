package com.flipkart.connekt.receptors.routes.reports

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.entities.Channel.Channel
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.directives.ChannelSegment
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
class ReportsRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler {

  val route = pathPrefix("v1" / "reports") {

    path(ChannelSegment / Segment / "messages" / Segment / Segment / "events") {
      (channel: Channel, appName: String, contactId: String, messageId: String) =>
        authorize(user, "REPORTS") {
          get {
            val events = ServiceFactory.getCallbackService.fetchCallbackEvent(messageId, contactId, channel).get
            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Events fetched for messageId: $messageId contactId: $contactId fetched.", Map(contactId -> events))))
          }
        }
    } ~ path(ChannelSegment / Segment / "messages" / Segment / "events") {
      (channel: Channel, appName: String, messageId: String) =>
        authorize(user, "REPORTS") {
          get {
            val events = ServiceFactory.getCallbackService.fetchCallbackEventByMId(messageId, channel).get
            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Events for $messageId fetched.", events)))

          }
        }
    } ~ path(ChannelSegment / Segment / "messages" / Segment) {
      (channel: Channel, appName: String, messageId: String) =>
        authorize(user, "REPORTS") {
          get {
            val data = ServiceFactory.getMessageService.getRequestInfo(messageId).get
            data match {
              case None =>
                complete(GenericResponse(StatusCodes.NotFound.intValue, Map("messageId" -> messageId), Response(s"No events found for messageId $messageId.", null)))
              case Some(x) =>
                complete(GenericResponse(StatusCodes.OK.intValue, Map("messageId" -> messageId), Response(s"Events fetched for messageId: $messageId.", data)))
            }
          }
        }
    }
  }
}
