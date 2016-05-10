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
package com.flipkart.connekt.receptors.routes.common

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.DeviceDetailsService
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class AdminRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    authenticate {
      user =>
        pathPrefix("v1") {
          pathPrefix("admin") {
            authorize(user, "ADMIN_CACHE_WARMUP") {
              pathPrefix("push" / "warmup") {
                path("jobs") {
                  get {
                    val result = DeviceDetailsService.cacheJobStatus.asScala.map {
                      case (appName, warmUpStatus) => appName ->
                        Map(
                          "currentCount" -> warmUpStatus.currentCount,
                          "completed" -> {
                            warmUpStatus.status.future.value match {
                              case None => "RUNNING"
                              case Some(Success(t)) => "COMPLETED"
                              case Some(Failure(error)) => "FAILED: " + error.getMessage
                            }
                          }
                        )
                    }
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Cache jobs status", Map("currentCount" -> result))))
                  }
                } ~
                  path(Segment) {
                    (appName: String) =>
                      get {
                        ConnektLogger(LogFile.SERVICE).info(s"REGISTRATION_CACHE_WARM_UP for $appName started by ${user.userId}")
                        val jobId = DeviceDetailsService.cacheWarmUp(appName)
                        complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"CacheWarmUp started", Map("appName" -> appName, "jobId" -> jobId))))
                      }
                  }
              }
            }
          }
        }
    }
}
