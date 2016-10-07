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
package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, _}
import akka.stream.{FanOutShape2, Inlet, Outlet, _}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.xmpp.XMPPStageLogic
import com.flipkart.connekt.commons.iomodels._

import scala.util.Try


class GcmXmppDispatcher(implicit actorSystem:ActorSystem) extends GraphStage[FanOutShape2[(FCMXmppRequest,GCMRequestTracker), (Try[XmppDownstreamResponse], GCMRequestTracker), XmppUpstreamResponse]] {

  val in = Inlet[(FCMXmppRequest,GCMRequestTracker)]("GcmXmppDispatcher.In")
  val outDownstream = Outlet[(Try[XmppDownstreamResponse], GCMRequestTracker)]("GcmXmppDispatcher.outDownstream")
  val outUpstream = Outlet[XmppUpstreamResponse]("GcmXmppDispatcher.outUpstream")
  override def shape = new FanOutShape2[(FCMXmppRequest,GCMRequestTracker), (Try[XmppDownstreamResponse], GCMRequestTracker), XmppUpstreamResponse](in, outDownstream, outUpstream)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new XMPPStageLogic(shape)
}
