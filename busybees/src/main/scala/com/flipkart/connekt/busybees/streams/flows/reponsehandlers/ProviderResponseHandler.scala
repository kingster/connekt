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

import com.flipkart.connekt.busybees.streams.flows.MapAsyncFlowStage
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent

trait ProviderResponseHandler

abstract class PNProviderResponseErrorHandler[I, O1](parallelism:Int = 128) extends  MapAsyncFlowStage[I, Either[O1, PNCallbackEvent]](parallelism) with ProviderResponseHandler

abstract class PNProviderResponseHandler[I](parallelism:Int = 128) extends MapAsyncFlowStage[I, PNCallbackEvent](parallelism) with ProviderResponseHandler
