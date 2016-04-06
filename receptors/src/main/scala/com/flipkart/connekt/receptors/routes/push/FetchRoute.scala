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
package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.entities.{AppUser, Channel}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.{ConnektConfig, StencilService}
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.util.Try

class   FetchRoute(implicit user: AppUser) extends BaseJsonHandler {

  val seenEventTypes = ConnektConfig.getList[String]("core.pn.seen.events")

  val fetch =
    pathPrefix("v1") {
      path("fetch" / "push" / MPlatformSegment / Segment / Segment) {
        (platform: MobilePlatform, appName: String, instanceId: String) =>
          authorize(user, "FETCH", s"FETCH_$appName") {
            get {
              parameters('startTs.as[Long], 'endTs ? System.currentTimeMillis, 'skipIds.*) { (startTs, endTs, skipIds) =>
                require(startTs < endTs, "startTs must be prior to endTs")
                require(startTs > (System.currentTimeMillis - 7.days.toMillis), "Invalid startTs : startTs can be max 7 days from now")

                val requestEvents = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(s"${appName.toLowerCase}$instanceId", Channel.PUSH, startTs, endTs)
                val messageService = ServiceFactory.getPNMessageService

                //Skip all messages which are either read/dismissed or passed in skipIds
                val skipMessageIds: Set[String] = skipIds.toSet ++ requestEvents.map(res => res.map(_.asInstanceOf[PNCallbackEvent]).filter(seenEventTypes.contains).map(_.messageId)).get.toSet
                val messages: Try[List[ConnektRequest]] = requestEvents.map(res => {
                  val messageIds = res.map(_.asInstanceOf[PNCallbackEvent]).map(_.messageId).distinct
                  val fetchedMessages = messageIds.filterNot(skipMessageIds.contains).flatMap(mId => messageService.getRequestInfo(mId).getOrElse(None))
                  val validMessages = fetchedMessages.filter(_.expiryTs.map(t => t > System.currentTimeMillis).getOrElse(true))
                  validMessages
                })

                val pushRequests = messages.get.map(r => {
                  val channelRequestData = r.templateId.flatMap(StencilService.get(_)).map(StencilService.render(_, r.channelDataModel)).getOrElse(r.channelData)
                  r.id -> channelRequestData
                }).toMap

                //TODO: Cleanup this.
                val finalTs = requestEvents.get.isEmpty match {
                  case false =>
                    val maxTimeStamp = requestEvents.get.maxBy(_.asInstanceOf[PNCallbackEvent].timestamp)
                    maxTimeStamp.asInstanceOf[PNCallbackEvent].timestamp
                  case true =>
                    endTs
                }

                complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched result for $instanceId", pushRequests))
                  .respondWithHeaders(Seq(RawHeader("endTs", finalTs.toString))))
              }
            }
          }
      }
    }
}
