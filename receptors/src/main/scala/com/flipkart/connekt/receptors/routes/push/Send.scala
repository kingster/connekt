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
class Send(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler {

  val send =
    pathPrefix("v1") {
      path("send" / "push" / "multicast" / MPlatformSegment / Segment) {
        (appPlatform: MobilePlatform, appName: String) =>
          authorize(user, "MULTICAST_" + appName) {
            post {
              entity(as[ConnektRequest]) { r =>
                ConnektLogger(LogFile.SERVICE).debug(s"Received multicast PN request with payload: ${r.toString}")

                /* Find platform for each deviceId, group */
                val pnRequestInfo = r.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName)
                val deviceIds = pnRequestInfo.deviceId
                val groupedPlatformRequests = ListBuffer[ConnektRequest]()

                appPlatform match {
                  case MobilePlatform.UNKNOWN =>
                    val groupedDevices = DeviceDetailsService.get(pnRequestInfo.appName, pnRequestInfo.deviceId).groupBy(_.osName).mapValues(_.map(_.deviceId))
                    groupedPlatformRequests ++= groupedDevices.map(kv => kv._1 -> r.copy(channelInfo = pnRequestInfo.copy(platform = kv._1, deviceId = kv._2))).values
                  case _ =>
                    groupedPlatformRequests += r
                }

                val failure = ListBuffer[String]()
                val success = scala.collection.mutable.Map[String, List[String]]()

                groupedPlatformRequests.toList.foreach { p =>
                  /* enqueue multiple requests into kafka */
                  ServiceFactory.getMessageService.persistRequest(p, "fk-connekt-pn", isCrucial = true) match {
                    case Success(id) =>
                      success += id -> p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                    case Failure(t) =>
                      failure ++= p.channelInfo.asInstanceOf[PNRequestInfo].deviceId
                  }
                }

                complete(GenericResponse(StatusCodes.Created.intValue, null, MulticastResponse("Multicast PN request processed.", success.toMap, failure.toList)).respond)
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
                  val unicastRequest = r.copy(channelInfo = pnRequestInfo)

                  ConnektLogger(LogFile.SERVICE).debug(s"Received unicast PN request with payload: ${r.toString}")
                  def enqueue = ServiceFactory.getMessageService.persistRequest(unicastRequest, "fk-connekt-pn", isCrucial = true)
                  async(enqueue) {
                    case Success(t) => t match {
                      case Success(requestId) =>
                        complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Unicast PN request enqueued for requestId: $requestId", null)).respond)
                      case Failure(e) =>
                        complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"Unicast PN request enqueue failed, e: ${e.getMessage}", null)).respond)
                    }
                    case Failure(e) =>
                      complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"Unicast PN request processing failed, e: ${e.getMessage}", null)).respond)
                  }
                }
              }
            }
        }
    }
}
