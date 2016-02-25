package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.stream.{FanOutShape2, FlowShape}
import akka.stream.stage.GraphStage
import com.flipkart.connekt.busybees.models.HTTPRequestTracker
import com.flipkart.connekt.commons.iomodels.{EmailCallbackEvent, CallbackEvent, PNCallbackEvent}

/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
trait ProviderResponseHandler

abstract class PNProviderResponseErrorHandler[I, O1] extends  GraphStage[FanOutShape2[I, PNCallbackEvent, O1]] with ProviderResponseHandler

abstract class PNProviderResponseHandler[I] extends GraphStage[FlowShape[I, PNCallbackEvent]] with ProviderResponseHandler

abstract class EmailProviderResponseHandler[I] extends GraphStage[FlowShape[I, EmailCallbackEvent]] with ProviderResponseHandler



