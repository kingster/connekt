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

import akka.stream.{FanOutShape2, FlowShape}
import akka.stream.stage.GraphStage
import com.flipkart.connekt.busybees.models.HTTPRequestTracker
import com.flipkart.connekt.busybees.streams.flows.MapFlowStage
import com.flipkart.connekt.commons.iomodels.{EmailCallbackEvent, CallbackEvent, PNCallbackEvent}

trait ProviderResponseHandler

abstract class PNProviderResponseErrorHandler[I, O1] extends  GraphStage[FanOutShape2[I, PNCallbackEvent, O1]] with ProviderResponseHandler

abstract class PNProviderResponseHandler[I] extends MapFlowStage[I, PNCallbackEvent] with ProviderResponseHandler

abstract class EmailProviderResponseHandler[I] extends MapFlowStage[I, EmailCallbackEvent] with ProviderResponseHandler
