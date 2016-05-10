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
                    val result = DeviceDetailsService.cacheJobStatus.map {
                      case (jobId, _) => jobId -> DeviceDetailsService.cacheWarmUpJobStatus(jobId)
                    }
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Registration cache warm-up jobs' status", result)))
                  }
                } ~
                  path(Segment) {
                    (appName: String) =>
                      get {
                        ConnektLogger(LogFile.SERVICE).info(s"Registration cache warm-up initiated for $appName by ${user.userId}")
                        val jobId = DeviceDetailsService.cacheWarmUp(appName)
                        complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Registration cache warm-up started", Map("appName" -> appName, "jobId" -> jobId))))
                      }
                  }
              }
            }
          }
        }
    }
}
