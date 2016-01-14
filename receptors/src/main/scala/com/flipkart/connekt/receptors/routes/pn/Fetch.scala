package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 1/14/16
 */
class Fetch extends BaseHandler {

  val fetch =
    pathPrefix("v1") {
      authenticate { user =>
        path("fetch" / "push" / Segment / Segment / Segment) {
          (platform: String, app: String, subscriberId: String) =>
            authorize(user, "FETCH", s"FETCH_$platform", s"FETCH_${platform}_$app") {
              get {
                parameters('startTs ?, 'endTs ?){ (startTs, endTs) =>
                  def fetchMessages = {
                    val requestEvents = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(subscriberId, "push")
                    val messageService = ServiceFactory.getMessageService

                    val messages: Try[List[ConnektRequest]] = requestEvents.map(res => {
                      val messageIds = res.map(_.asInstanceOf[PNCallbackEvent].messageId).distinct
                      messageIds.flatMap(mId => messageService.getRequestInfo(mId).getOrElse(None))
                    })
                    messages.get
                  }

                  val pushRequests = fetchMessages.map(r => r.id -> r.channelData.asInstanceOf[PNRequestData]).toMap

                  complete(respond[GenericResponse](
                    StatusCodes.Created, Seq.empty[HttpHeader],
                    GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetch result for $subscriberId", pushRequests))
                  ))
                }
              }
            }
        }
      }
    }
}
