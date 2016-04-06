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
package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

trait AccessLogDirective extends BasicDirectives {

  private val logFormat = "%s %s %s %s %s"

  // logs just the request method and response status at info level
  private def logRequestAndResponse(req: HttpRequest): Any => Unit = {
    case (routeResult: Complete, time: Long) =>
      val remoteIp: String = req.headers.find(_.is("fk-client-ip")).map(_.value()).getOrElse("0.0.0.0")
      ConnektLogger(LogFile.ACCESS).info(logFormat.format(remoteIp, req.method.value, req.uri, routeResult.response.status.intValue(), time))
    case _ =>
  }

  /**
   * Produces a log entry for every incoming request and [[RouteResult]].
   */
  def logTimedRequestResult: Directive0 =
    extractRequestContext.flatMap { ctx ⇒
      val startTs = System.currentTimeMillis
      mapRouteResult { result ⇒
        val timeTaken = System.currentTimeMillis - startTs
        logRequestAndResponse(ctx.request)((result, timeTaken))
        result
      }
    }


}
