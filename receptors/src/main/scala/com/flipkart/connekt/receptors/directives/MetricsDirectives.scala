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

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.directives.BasicDirectives
import com.codahale.metrics.Timer
import com.flipkart.connekt.commons.metrics.Instrumented

trait MetricsDirectives extends BasicDirectives with Instrumented {

  def meteredResource(resourceId: String): Directive0 =
    extractRequestContext.flatMap { ctx =>
      val context: Timer.Context = registry.timer(getMetricName(resourceId)).time()
      mapResponse { r =>
        context.stop()
        counter(s"$resourceId.${r.status.intValue()}").inc()
        r
      }
    }
}
