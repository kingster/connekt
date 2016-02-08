package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.stream.FlowShape
import akka.stream.stage.GraphStage
import com.flipkart.connekt.commons.iomodels.{EmailCallbackEvent, CallbackEvent, PNCallbackEvent}

/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
abstract class ProviderResponseHandler[T, U <: CallbackEvent] extends GraphStage[FlowShape[T, List[U]]]
abstract class PNProviderResponseHandler[T] extends ProviderResponseHandler[T, PNCallbackEvent]
abstract class EmailProviderResponseHandler[T] extends ProviderResponseHandler[T, EmailCallbackEvent]