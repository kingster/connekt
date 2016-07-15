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

import akka.actor.{ActorSystem, ActorRef}
import akka.stream.{Inlet, Outlet, FanOutShape2}
import akka.stream.stage.{AsyncCallback, GraphStage}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.xmpp.XmppGatewayCache
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._
import akka.stream._
import akka.stream.stage._
import com.flipkart.connekt.commons.services.ConnektConfig
import scala.util.{Success, Try}

class GcmXmppDispatcher(implicit actorSystem:ActorSystem) extends GraphStage[FanOutShape2[(GcmXmppRequest,GCMRequestTracker), (Try[XmppDownstreamResponse], GCMRequestTracker), XmppUpstreamResponse]] {

  val maxPendingUpstreamCount = ConnektConfig.get("fcm.xmpp.maximumUpstreamCount").getOrElse("10").toInt

  val in = Inlet[(GcmXmppRequest,GCMRequestTracker)]("GcmXmppDispatcher.In")
  val outDownstream = Outlet[(Try[XmppDownstreamResponse], GCMRequestTracker)]("GcmXmppDispatcher.outDownstream")
  val outUpstream = Outlet[XmppUpstreamResponse]("GcmXmppDispatcher.outUpstream")
  override def shape = new FanOutShape2[(GcmXmppRequest,GCMRequestTracker), (Try[XmppDownstreamResponse], GCMRequestTracker), XmppUpstreamResponse](in, outDownstream, outUpstream)

  //to ask for more from inq
  var getMoreCallback: AsyncCallback[String] = null

  //to push downstream
  var ackRecvdCallback: AsyncCallback[(Try[XmppDownstreamResponse], GCMRequestTracker)] = null

  //to push upstream data
  var upStreamRecvdCallback: AsyncCallback[(ActorRef,XmppUpstreamResponse)] = null

  var retryCallback: AsyncCallback[(GcmXmppRequest,GCMRequestTracker)] = null

  val xmppState = new XmppGatewayCache(this)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    override def preStart(): Unit = {
      getMoreCallback = getAsyncCallback[String] {
        appId => {
          ConnektLogger(LogFile.CLIENTS).trace("Received pull request:" + appId)
          xmppState.incrementConnectionFreeCount(appId)
          if ( !hasBeenPulled(in) ) {
            pull(in)
            ConnektLogger(LogFile.CLIENTS).trace("Pulled in:" + appId)
          }
        }
      }

      retryCallback = getAsyncCallback[(GcmXmppRequest,GCMRequestTracker)] {
        request => {
          xmppState.addIncomingRequest(request)
        }
      }

      ackRecvdCallback = getAsyncCallback[(Try[XmppDownstreamResponse], GCMRequestTracker)] {
        downstreamResponse => {
          xmppState.enqueueDownstream(downstreamResponse)
          if ( isAvailable(outDownstream))
            push(outDownstream, xmppState.dequeueDownstream)
        }
      }

      upStreamRecvdCallback = getAsyncCallback[(ActorRef,XmppUpstreamResponse)] {
        upstreamResponse => {
          xmppState.enqueueUpstream(upstreamResponse)
          if ( isAvailable(outUpstream) ) {
            val (xmppActor, upstream) = xmppState.responsesUpStream.dequeue()
            xmppActor ! upstream
            push(outUpstream, upstream)
          }
        }
      }

      pull(in)
    }

    val inhandler = new InHandler {
      override def onPush(): Unit = {
        val requestPair:(GcmXmppRequest,GCMRequestTracker) = grab(in)
        xmppState.addIncomingRequest(requestPair)

        //exhaust buffer as long as connections available
        xmppState.sendRequests

        //pull more if connections available
        if ( xmppState.responsesUpStream.size < maxPendingUpstreamCount && xmppState.connectionAvailable > 0 )
          pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        //TODO NEED ALERT HERE
        //("INPUT STREAM IS CLOSED")
      }
    }
    setHandler(in, inhandler)

    val downstreamOutHandler = new OutHandler {
      override def onPull(): Unit = {
        if ( xmppState.responsesDownStream.nonEmpty )
          push(outDownstream, xmppState.responsesDownStream.dequeue())

        if ( xmppState.responsesUpStream.size < maxPendingUpstreamCount && xmppState.connectionAvailable > 0 && !hasBeenPulled(in))
          pull(in)
      }
    }
    setHandler(outDownstream, downstreamOutHandler)

    val upstreamUphandler = new OutHandler {
      override def onPull(): Unit = {
        if (xmppState.responsesUpStream.nonEmpty) {
          val (upstreamActor, upstreamResponse) = xmppState.responsesUpStream.dequeue()
          upstreamActor ! upstreamResponse
          push(outUpstream, upstreamResponse)
        }
        if (xmppState.responsesUpStream.size < maxPendingUpstreamCount && xmppState.connectionAvailable > 0 && !hasBeenPulled(in))
          pull(in)
      }
    }
    setHandler(outUpstream, upstreamUphandler)
  }
}
