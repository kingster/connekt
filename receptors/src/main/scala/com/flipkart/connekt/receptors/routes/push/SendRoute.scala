package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.entities.{AppUser, MobilePlatform}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
class SendRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler {

  val route =
    pathPrefix("v1") {
      path("send" / "push" / "multicast" / MPlatformSegment / Segment) {
        (appPlatform: MobilePlatform, appName: String) =>
          authorize(user, "MULTICAST_" + appName) {
            post {
              entity(as[ConnektRequest]) { multicastRequest =>
                ConnektLogger(LogFile.SERVICE).debug(s"Received multicast PN request with payload: ${multicastRequest.toString}")

                /* Find platform for each deviceId, group */
                val pnRequestInfo = multicastRequest.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName)
                val deviceIds = pnRequestInfo.deviceId
                val groupedPlatformRequests = ListBuffer[ConnektRequest]()

                appPlatform match {
                  case MobilePlatform.UNKNOWN =>
                    val groupedDevices = DeviceDetailsService.get(pnRequestInfo.appName, pnRequestInfo.deviceId).get.groupBy(_.osName).mapValues(_.map(_.deviceId))
                    groupedPlatformRequests ++= groupedDevices.map { case (platform, deviceId) =>
                      platform -> multicastRequest.copy(channelInfo = pnRequestInfo.copy(platform = platform, deviceId = deviceId))
                    }.values
                  case _ =>
                    groupedPlatformRequests += multicastRequest
                }

                val failure = ListBuffer[String]()
                val success = scala.collection.mutable.Map[String, List[String]]()

                val queueName = ServiceFactory.getMessageService.getRequestBucket(multicastRequest.copy(channel = "PN"), user)

                groupedPlatformRequests.toList.foreach { p =>
                  /* enqueue multiple requests into kafka */
                  ServiceFactory.getMessageService.persistRequest(p, queueName, isCrucial = true) match {
                    case Success(id) =>
                      success += id -> p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                    case Failure(t) =>
                      failure ++= p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                  }
                }

                complete(GenericResponse(StatusCodes.Created.intValue, null, MulticastResponse("Multicast PN request processed.", success.toMap, failure.toList)))
              }
            }
          }
      } ~
        path("send" / "push" / "unicast" / MPlatformSegment / Segment) {
          (appPlatform: MobilePlatform, appName: String) =>
            authorize(user, "UNICAST_" + appName) {
              post {
                entity(as[ConnektRequest]) { r =>
                  val pnRequestInfo = r.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName, platform = appPlatform.toString)
                  val unicastRequest = r.copy(channelInfo = pnRequestInfo, channel = "PN")

                  ConnektLogger(LogFile.SERVICE).debug(s"Received unicast PN request with payload: ${r.toString}")
                  val queueName = ServiceFactory.getMessageService.getRequestBucket(unicastRequest, user)
                  val requestId = ServiceFactory.getMessageService.persistRequest(unicastRequest, queueName, isCrucial = true).get
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Unicast PN request enqueued for requestId: $requestId", null)))

                }
              }
            }
        }
    }
}
