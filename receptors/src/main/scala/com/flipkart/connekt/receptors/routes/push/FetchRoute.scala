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

import akka.connekt.AkkaHelpers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try


class FetchRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val seenEventTypes = ConnektConfig.getList[String]("core.pn.seen.events").map(_.toLowerCase)

  implicit val ioDispatcher = am.getSystem.dispatchers.lookup("akka.actor.route-blocking-dispatcher")

  lazy implicit val stencilService = ServiceFactory.getStencilService

  val route =
    authenticate {
      user =>
        pathPrefix("v1") {
          path("fetch" / "push" / MPlatformSegment / Segment / Segment) {
            (platform: MobilePlatform, appName: String, instanceId: String) =>
              authorize(user, "FETCH", s"FETCH_$appName") {
                get {
                  parameters('startTs.as[Long], 'endTs ? System.currentTimeMillis, 'skipIds.*) {(startTs, endTs, skipIds) =>
                    complete {
                      Future {
                        profile(s"fetch.$platform.$appName") {

                          require(startTs < endTs, "startTs must be prior to endTs")

                          val safeStartTs = if(startTs < (System.currentTimeMillis - 7.days.toMillis)) System.currentTimeMillis - 1.days.toMillis else startTs
                          val requestEvents = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(s"${appName.toLowerCase}$instanceId", Channel.PUSH, safeStartTs + 1, endTs)
                          val messageService = ServiceFactory.getPNMessageService

                          //Skip all messages which are either read/dismissed or passed in skipIds
                          val skipMessageIds: Set[String] = skipIds.toSet ++ requestEvents.map(res => res.map(_._1.asInstanceOf[PNCallbackEvent]).filter(e => seenEventTypes.contains(e.eventType.toLowerCase)).map(_.messageId)).get.toSet
                          val messages: Try[List[ConnektRequest]] = requestEvents.map(res => {
                            val messageIds: List[String] = res.map(_._1.asInstanceOf[PNCallbackEvent]).map(_.messageId).distinct
                            val filteredMessageIds: List[String] = messageIds.filterNot(skipMessageIds.contains)
                            val fetchedMessages: Try[List[ConnektRequest]] = messageService.getRequestInfo(filteredMessageIds)
                            fetchedMessages.map(_.filter(_.expiryTs.forall(_ >= System.currentTimeMillis)).filterNot(_.isTestRequest)).getOrElse(List.empty[ConnektRequest])
                          })

                          val pushRequests = messages.get.map(r => {
                            r.id -> {
                              val channelData = Option(r.channelData) match {
                                case Some(PNRequestData(_, pnData)) if pnData != null => r.channelData
                                case _ => r.getComputedChannelData
                              }
                              val pnRequestData = channelData.asInstanceOf[PNRequestData]
                              pnRequestData.data.put("contextId", r.contextId.orEmpty).put("messageId", r.id)
                            }
                          }).toMap

                          val transformedRequests = stencilService.getStencilsByName(s"ckt-${appName.toLowerCase}-fetch").headOption match {
                            case None => pushRequests
                            case Some(stencil) => stencilService.materialize(stencil, pushRequests.getJsonNode)
                          }

                          val finalTs = requestEvents.getOrElse(List.empty[(CallbackEvent, Long)]).map(_._2).reduceLeftOption(_ max _).getOrElse(endTs)

                          GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched result for $instanceId", transformedRequests))
                            .respondWithHeaders(scala.collection.immutable.Seq(RawHeader("endTs", finalTs.toString), RawHeader("Access-Control-Expose-Headers", "endTs")))
                        }
                      }(ioDispatcher)
                    }
                  }
                }
              }
          }
        }
    }
}
