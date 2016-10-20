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
import com.flipkart.connekt.commons.entities.bigfoot.PublishSupport
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.BigfootService
import com.flipkart.connekt.commons.utils.StringUtils._

class SpecterSink extends Instrumented {
  def sink = {
    Sink.foreach[SubscriptionEvent](e => {
      val callbackEvent = (e.payload match {
        case str: String => str
        case _ => e.payload.getJson
      }).getObj[CallbackEvent]

      callbackEvent match {
        case bfSupported: PublishSupport =>
          BigfootService.ingestEvent(bfSupported.toPublishFormat, callbackEvent.namespace)
          meter(s"firefly.specter.bf.ingested").mark()
        case _ => meter(s"firefly.specter.bf.skipped").mark()
      }
    })
  }
}
