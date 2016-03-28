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
package com.flipkart.connekt.receptors.routes.status

import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.service.HealthService
import com.flipkart.connekt.receptors.service.HealthService.ServiceStatus

import scala.collection.immutable.Seq

class SystemStatus(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    path("elb-healthcheck") {
      get {
        HealthService.getStatus match {
          case ServiceStatus.IN_ROTATION =>
            complete(responseMarshallable(StatusCodes.OK.intValue,Seq.empty[HttpHeader],HealthService.elbResponse()))
          case ServiceStatus.OUT_OF_ROTATION =>
            complete(responseMarshallable(StatusCodes.ServiceUnavailable.intValue,Seq.empty[HttpHeader],HealthService.elbResponse()))
        }
      }
    } ~ pathPrefix("service") {
      path("oor"){
        HealthService.oor()
        complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Service Status Moved to OOR", Map("status" -> HealthService.getStatus))))
      } ~ path("bir") {
        HealthService.bir()
        complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Service Status Moved to IR", Map("status" -> HealthService.getStatus))))
      }
    }
}

