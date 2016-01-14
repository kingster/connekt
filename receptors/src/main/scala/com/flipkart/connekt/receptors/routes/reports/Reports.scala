package com.flipkart.connekt.receptors.routes.reports

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
class Reports(implicit am: ActorMaterializer) extends BaseHandler {

  val route =
    pathPrefix("v1") {
      authenticate {
        user =>
          path(Segment / "events" / Segment / Segment) {
            (channel: String, contactId: String, messageId: String) =>
              authorize(user, "REPORTS") {
                get {
                  val events = ServiceFactory.getCallbackService.fetchCallbackEvent(messageId, contactId, channel).get
                  complete(respond[GenericResponse](
                    StatusCodes.OK, Seq.empty[HttpHeader],
                    GenericResponse(StatusCodes.OK.intValue, null, Response(s"Events for $messageId $contactId fetched.", Map(contactId -> events)))
                  ))
                }
              }
          } ~ path(Segment / "status" / Segment) {
            (channel: String, messageId: String) =>
              authorize(user, "REPORTS") {
                get {
                  val data = ServiceFactory.getMessageService.getRequestInfo(messageId).get
                  data match {
                    case None =>
                      complete(respond[GenericResponse](
                        StatusCodes.NotFound, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.NotFound.intValue, Map("messageId" -> messageId), Response(s"No Data found for MessageId $messageId", null))
                      ))
                    case Some(x) =>
                      complete(respond[GenericResponse](
                        StatusCodes.OK, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.OK.intValue, Map("messageId" -> messageId), Response(s"Details for $messageId.", data))
                      ))
                  }
                }
              }
          }
      }
    }
}
