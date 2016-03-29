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
import com.flipkart.connekt.commons.services.BigfootService
/**
 * Created by kinshuk.bairagi on 29/03/16.
 */
trait CallbackRecorder {

  implicit class PNListCallbackRecorder(val events: Iterable[PNCallbackEvent]) {
    def persist = Try_ {
      events.foreach(e => {
        ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, s"${e.appName}${e.deviceId}", Channel.PUSH, e)
        BigfootService.ingest(e.toBigfootFormat)
      })
    }
  }


}
