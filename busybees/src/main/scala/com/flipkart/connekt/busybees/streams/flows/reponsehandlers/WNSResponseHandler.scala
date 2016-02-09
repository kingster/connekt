package com.flipkart.connekt.busybees.streams.flows.reponsehandlers

import akka.http.scaladsl.model.HttpResponse
import akka.stream.stage.GraphStageLogic
import akka.stream._
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 2/8/16
 */
class WNSResponseHandler(implicit m: Materializer, ec: ExecutionContext) extends PNProviderResponseHandler[(Try[HttpResponse], String)] {

  val in = Inlet[(Try[HttpResponse], String)]("WNSResponseHandler.In")
  val out = Outlet[PNCallbackEvent]("WNSResponseHandler.Out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = ???

  override def shape  = FlowShape.of(in, out)
}
