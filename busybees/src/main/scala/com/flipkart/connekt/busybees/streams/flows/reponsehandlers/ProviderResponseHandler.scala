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

import akka.stream.FanOutShape2
import akka.stream.stage.GraphStage
import com.flipkart.connekt.busybees.streams.flows.{MapAsyncFlowStage, MapFlowStage}
import com.flipkart.connekt.commons.iomodels.{EmailCallbackEvent, PNCallbackEvent}

trait ProviderResponseHandler

abstract class PNProviderResponseErrorHandler[I, O1] extends  GraphStage[FanOutShape2[I, PNCallbackEvent, O1]] with ProviderResponseHandler

abstract class PNProviderResponseHandler[I](parallelism:Int = 128) extends MapAsyncFlowStage[I, PNCallbackEvent](parallelism) with ProviderResponseHandler

abstract class EmailProviderResponseHandler[I](parallelism:Int = 128) extends MapAsyncFlowStage[I, EmailCallbackEvent](parallelism) with ProviderResponseHandler
