package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.Try
import com.flipkart.connekt.commons.entities.{AppUser, Channel}

/**
 *
 *
 * @author durga.s
 * @version 1/14/16
 */
class Fetch(implicit user: AppUser) extends BaseHandler {

  val fetch =
    pathPrefix("v1") {
        path("fetch" / "push" / MPlatformSegment / Segment / Segment) {
          (platform: MobilePlatform, app: String, subscriberId: String) =>
            authorize(user, "FETCH", s"FETCH_${platform.toString}", s"FETCH_${platform.toString}_$app") {
              get {
                parameters('startTs ?, 'endTs ?){ (startTs, endTs) =>
                  def fetchMessages = {
                    val startTime = startTs.map(_.toLong).getOrElse(System.currentTimeMillis() - 7 * 24 * 3600 * 1000)
                    val endTime = endTs.map(_.toLong).getOrElse(System.currentTimeMillis())

                    val requestEvents = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(subscriberId, Channel.PUSH, startTime, endTime)
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
