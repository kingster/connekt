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
package com.flipkart.connekt.busybees.streams

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, Attributes, Materializer, Supervision}
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{CallbackEvent, ConnektRequest, PNCallbackEvent}
import com.flipkart.connekt.commons.services.BigfootService

trait ConnektTopology[E <:CallbackEvent] {
  def source: Source[ConnektRequest, NotUsed]
  def transform: Flow[ConnektRequest, E, NotUsed]
  def sink: Sink[E, NotUsed]

  def graph() = source.withAttributes(ActorAttributes.dispatcher("akka.actor.default-pinned-dispatcher")).via(transform).to(sink)
    .withAttributes(ActorAttributes.supervisionStrategy(stageSupervisionDecider))
//    .withAttributes(Attributes.inputBuffer(initial = 16, max = 64))
    .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel, onFinish = Logging.InfoLevel, onFailure = Logging.ErrorLevel))

  def run(implicit mat: Materializer) = graph().run()
  def shutdown()

  val stageSupervisionDecider: Supervision.Decider = {
    case cEx: ConnektPNStageException =>
      val callbackEvents = cEx.deviceId.map(PNCallbackEvent(cEx.messageId, _, cEx.eventType, cEx.platform, cEx.appName, cEx.context, cEx.getMessage, cEx.timeStamp))
      callbackEvents.foreach(e => ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, s"${e.appName}${e.deviceId}", Channel.PUSH, e))
      callbackEvents.foreach(e => BigfootService.ingest(e.toBigfootFormat))
      Supervision.Resume

    case _ => Supervision.Stop
  }
}
