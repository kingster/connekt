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
package com.flipkart.connekt.firefly.sinks.specter

import akka.stream.scaladsl.Sink
import com.flipkart.connekt.commons.entities.SubscriptionEvent
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.BigfootService
import com.flipkart.connekt.commons.utils.StringUtils._

class SpecterSink extends Instrumented {
  def sink = {
    Sink.foreach[SubscriptionEvent](e => {
      BigfootService.ingest((e.payload match {
        case str: String => str
        case _ => e.payload.getJson
      }).getObj[PNCallbackEvent].toBigfootFormat
      )
      meter(s"firefly.specter.rate").mark()
    })
  }
}
