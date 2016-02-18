package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.entities.{AppUser, Channel}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 1/14/16
 */
class FetchRoute(implicit user: AppUser) extends BaseHandler {

  val fetch =
    pathPrefix("v1") {
      path("fetch" / "push" / MPlatformSegment / Segment / Segment) {
        (platform: MobilePlatform, app: String, instanceId: String) =>
          authorize(user, "FETCH", s"FETCH_${platform.toString}", s"FETCH_${platform.toString}_$app") {
            get {
              parameters('startTs.as[Long], 'endTs ? System.currentTimeMillis()) { (startTs, endTs) =>

                val requestEvents = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(instanceId, Channel.PUSH, startTs, endTs)
                val messageService = ServiceFactory.getPNMessageService

                val messages: Try[List[ConnektRequest]] = requestEvents.map(res => {
                  val messageIds = res.map(_.asInstanceOf[PNCallbackEvent].messageId).distinct
                  messageIds.flatMap(mId => messageService.getRequestInfo(mId).getOrElse(None))
                })

                val pushRequests = messages.get.map(r => r.id -> r.channelData.asInstanceOf[PNRequestData]).toMap

                complete(
                  GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched result for $instanceId", pushRequests))
                    .respondWithHeaders(Seq(RawHeader("endTs",endTs.toString)))
                )
              }
            }
          }
      }
    }
}
