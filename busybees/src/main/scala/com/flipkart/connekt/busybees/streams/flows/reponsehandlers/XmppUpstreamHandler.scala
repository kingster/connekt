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
package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.stream.Materializer
import com.flipkart.connekt.commons.iomodels.{XmppUpstreamResponse, PNCallbackEvent, XmppReceipt, XmppUpstreamData}

import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.metrics.Instrumented
import scala.concurrent.{Future, ExecutionContext}

class XmppUpstreamHandler (implicit m: Materializer) extends PNProviderResponseHandler[XmppUpstreamResponse](4) with Instrumented {

  override val map: (XmppUpstreamResponse) => Future[List[PNCallbackEvent]] = upstreamResponse => Future({
    upstreamResponse.getPnCallbackEvent() map { event =>
        val events = List(event)
        events.persist
        events
    }
  }.getOrElse(List()))(m.executionContext)
}