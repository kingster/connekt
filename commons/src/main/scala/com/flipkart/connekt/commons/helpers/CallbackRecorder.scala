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
package com.flipkart.connekt.commons.helpers

import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.metrics.Instrumented

object CallbackRecorder extends Instrumented {

  implicit def callbackRecorder(event: PNCallbackEvent): PNListCallbackRecorder = new PNListCallbackRecorder(List(event))

  implicit class PNListCallbackRecorder(val events: Iterable[PNCallbackEvent]) {
    def persist = Try_ {
      events.foreach(e => {
        ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, s"${e.appName.toLowerCase}${e.deviceId}", Channel.PUSH, e)
        meter(s"event.${e.eventType}").mark()
//        BigfootService.ingest(e.toBigfootFormat)
      })
    }
  }
}
