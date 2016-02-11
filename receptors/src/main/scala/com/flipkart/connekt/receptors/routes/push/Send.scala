package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.entities.{AppUser, MobilePlatform}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
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
        (platform: MobilePlatform, appName: String) =>
          authorize(user, "MULTICAST_" + appName) {
            post {
              entity(as[ConnektRequest]) { r =>
                ConnektLogger(LogFile.SERVICE).debug(s"Received unicast PN request with payload: ${r.toString}")

                /* Find platform for each deviceId, group */
                val pnRequestInfo = r.channelInfo.asInstanceOf[PNRequestInfo].copy(appName = appName)
                val deviceIds = pnRequestInfo.deviceId
                val groupedPlatformRequests = ListBuffer[ConnektRequest]()

                platform match {
                  case MobilePlatform.UNKNOWN =>
                    val w = deviceIds.groupBy(DeviceDetailsService.get(pnRequestInfo.appName, _).map(_.osName).getOrElse("__MISSING__"))
                    groupedPlatformRequests ++= w.filterKeys(!_.equalsIgnoreCase("__MISSING__")).map(kv => {
                      r.copy(channelInfo = pnRequestInfo.copy(deviceId = kv._2, platform = kv._1))
                    })
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

                complete(respond[GenericResponse](
                  StatusCodes.OK, Seq.empty[HttpHeader],
                  GenericResponse(StatusCodes.OK.intValue, null, MulticastResponse("Multicast request processed.", success.toMap, failure.toList))
                ))
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
                        complete(respond[GenericResponse](
                          StatusCodes.Created, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.OK.intValue, null, Response("PN Request en-queued successfully for %s".format(requestId), null))
                        ))
                      case Failure(e) =>
                        complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("PN Request processing failed: %s".format(e.getMessage), null))
                        ))
                    }
                    case Failure(e) =>
                      complete(respond[GenericResponse](
                        StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("PN Request processing failed: %s".format(e.getMessage), null))
                      ))
                  }
                }
              }
            }
        }
    }
}
