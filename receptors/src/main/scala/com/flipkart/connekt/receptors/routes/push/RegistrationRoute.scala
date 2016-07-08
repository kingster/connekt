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

import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.{ConnektConfig, DeviceDetailsService}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._
import com.flipkart.utils.http.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RegistrationRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  @deprecated
  val isVaradhiRelayEnabled = ConnektConfig.getBoolean("flags.varadhi.enabled").get

  @deprecated
  val varadhiUri = s"http://${ConnektConfig.getString("connections.varadhi.host").get}/topics/${ConnektConfig.getString("connections.varadhi.topic").get}/messages"

  @deprecated
  val client = new HttpClient(
    name = "varadhi-relay-client",
    ttlInMillis = ConnektConfig.getInt("http.apache.ttlInMillis").getOrElse(60000).toLong,
    maxConnections = ConnektConfig.getInt("http.apache.maxConnections").getOrElse(50),
    processQueueSize = ConnektConfig.getInt("http.apache.processQueueSize").getOrElse(50),
    connectionTimeoutInMillis = ConnektConfig.getInt("http.apache.connectionTimeoutInMillis").getOrElse(30000).toInt,
    socketTimeoutInMillis = ConnektConfig.getInt("http.apache.socketTimeoutInMillis").getOrElse(60000).toInt
  )

  val route =
    authenticate {
      user =>
        pathPrefix("v1") {
          pathPrefix("registration" / "push") {
            path(MPlatformSegment / Segment / Segment) {
              (platform: MobilePlatform, appName: String, deviceId: String) =>
                verifySecureCode(appName.toLowerCase, user.apiKey, deviceId) {
                  put {
                    meteredResource(s"register.$platform.$appName") {
                      authorize(user, "REGISTRATION", s"REGISTRATION_$appName") {
                        entity(as[DeviceDetails]) { d =>
                          val newDeviceDetails = d.copy(appName = appName, osName = platform.toString, deviceId = deviceId, active = true)
                          newDeviceDetails.validate()

                          val result = DeviceDetailsService.get(appName, deviceId).transform[Either[Unit, Unit]]({
                            case Some(deviceDetail) => DeviceDetailsService.update(deviceId, newDeviceDetails).map(u => Left(Unit))
                            case None => DeviceDetailsService.add(newDeviceDetails).map(c => Right(Unit))
                          }, Failure(_)).get

                          /* temporary workaround to enable relay of registration */
                          if(isVaradhiRelayEnabled) {
                            Future {
                              val request = new HttpPost(varadhiUri)
                              request.setHeader("X_EVENT_TYPE", "REGISTRATION")
                              request.setHeader("X_RESTBUS_MESSAGE_ID", newDeviceDetails.deviceId)
                              request.setEntity(new StringEntity(newDeviceDetails.copy(token = null).getJson))

                              client.doExecute(request) match {
                                case Success(r) =>
                                  ConnektLogger(LogFile.SERVICE).info(s"DeviceDetails relayed for ${newDeviceDetails.deviceId}: ${r.getStatusLine.getStatusCode} ${r.getEntity.getContent.getString.replaceAll("\n", "")}")
                                case Failure(t) =>
                                  ConnektLogger(LogFile.SERVICE).error(s"DeviceDetails relay failure for ${newDeviceDetails.deviceId}", t)
                              }
                            }(executor = ExecutionContext.global)
                          }
                          /* , shall be removed like it never existed */

                          result match {
                            case Right(x) =>
                              complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"DeviceDetails created for ${newDeviceDetails.deviceId}", newDeviceDetails)))
                            case Left(x) =>
                              complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails updated for ${newDeviceDetails.deviceId}", newDeviceDetails)))
                          }
                        }
                      }
                    }
                  } ~ delete {
                    meteredResource(s"unregister.$platform.$appName") {
                      authorize(user, "REGISTRATION", s"REGISTRATION_$appName") {
                        DeviceDetailsService.get(appName, deviceId).get match {
                          case Some(device) =>
                            DeviceDetailsService.delete(appName, deviceId).get
                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails deleted for $deviceId", null)))
                          case None =>
                            complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"No Device Found for $appName / $deviceId", null)))
                        }
                      }
                    }
                  }
                }
            } ~ path(Segment / "users" / Segment) {
              (appName: String, userId: String) =>
                get {
                  meteredResource(s"getUserDevices.$appName") {
                    authorize(user, "REGISTRATION_READ", s"REGISTRATION_READ_$appName") {
                      val deviceDetails = DeviceDetailsService.getByUserId(appName, userId).get
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails fetched for app: $appName, user: $userId", Map[String, Any]("deviceDetails" -> deviceDetails))))
                    }
                  }
                }
            } ~ path(MPlatformSegment / Segment / Segment) {
              (platform: MobilePlatform, appName: String, deviceId: String) =>
                get {
                  meteredResource(s"getRegistration.$platform.$appName") {
                    authorize(user, "REGISTRATION_READ", s"REGISTRATION_READ_$appName") {
                      DeviceDetailsService.get(appName, deviceId).get match {
                        case Some(deviceDetail) =>
                          complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"DeviceDetails fetched for app: $appName id: $deviceId", Map[String, Any]("deviceDetails" -> deviceDetail))))
                        case None =>
                          complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"No DeviceDetails found for app: $appName id: $deviceId", null)))
                      }
                    }
                  }
                }
            } ~ path(Segment / "snapshot") {
              (appName: String) =>
                get {
                  meteredResource(s"getAllRegistrations.$appName") {
                    authorize(user, "REGISTRATION_DOWNLOAD", s"REGISTRATION_DOWNLOAD_$appName") {
                      ConnektLogger(LogFile.SERVICE).info(s"REGISTRATION_DOWNLOAD for $appName started by ${user.userId}")
                      val dataStream = DeviceDetailsService.getAll(appName).get
                      def chunks = Source.fromIterator(() => dataStream)
                        .grouped(100)
                        .map(d => d.map(_.getJson).mkString(scala.compat.Platform.EOL))
                        .map(HttpEntity.ChunkStreamPart.apply)
                      val response = HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunks))
                      complete(response)
                    }
                  }
                }
            }
          }
        }
    }
}


