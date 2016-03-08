package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.entities.{AppUser, Channel}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 1/14/16
 */
class FetchRoute(implicit user: AppUser) extends BaseJsonHandler {

  val seenEventTypes = ConnektConfig.getList[String]("core.pn.seen.events")

  val fetch =
    pathPrefix("v1") {
      path("fetch" / "push" / MPlatformSegment / Segment / Segment) {
        (platform: MobilePlatform, app: String, instanceId: String) =>
          authorize(user, "FETCH", s"FETCH_${platform.toString}", s"FETCH_${platform.toString}_$app") {
            get {
              parameters('startTs.as[Long], 'endTs ? System.currentTimeMillis(), 'skipIds.*) { (startTs, endTs, skipIds) =>

                //return if startTs is older than 7 days
                System.currentTimeMillis() - 7.days.toMillis > startTs match {
                  case false =>
                    val requestEvents = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(s"$app$instanceId", Channel.PUSH, startTs, endTs)
                    val messageService = ServiceFactory.getPNMessageService

                    //Skip all messages which are either read/dismissed or passed in skipIds
                    val skipMessageIds: Set[String] = skipIds.toSet ++ requestEvents.map(res => res.map(_.asInstanceOf[PNCallbackEvent]).filter(seenEventTypes.contains).map(_.messageId)).get.toSet
                    val messages: Try[List[ConnektRequest]] = requestEvents.map(res => {
                      val messageIds = res.map(_.asInstanceOf[PNCallbackEvent]).map(_.messageId).distinct
                      messageIds.filterNot(skipMessageIds.contains).flatMap(mId => messageService.getRequestInfo(mId).getOrElse(None))
                    })

                    val pushRequests = messages.get.map(r => r.id -> r.channelData.asInstanceOf[PNRequestData].data).toMap

                    val finalTs =  requestEvents.get.isEmpty match {
                      case false =>
                        val maxTimeStamp = requestEvents.get.maxBy(_.asInstanceOf[PNCallbackEvent].timestamp)
                        maxTimeStamp.asInstanceOf[PNCallbackEvent].timestamp
                      case true =>
                        endTs
                    }
                    complete(
                      GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched result for $instanceId", pushRequests))
                        .respondWithHeaders(Seq(RawHeader("endTs", finalTs.toString)))
                    )

                  case true =>
                    ConnektLogger(LogFile.SERVICE).error(s"Invalid fetch push startTs Request for $startTs ms ")
                    complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Invalid startTs : API support startTs only last 7 days from current time", null)))
                }
              }
            }
          }
      }
    }
}
