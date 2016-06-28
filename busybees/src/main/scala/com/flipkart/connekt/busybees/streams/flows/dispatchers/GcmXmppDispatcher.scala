package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.actor.ActorRef
import akka.stream.{Inlet, Outlet, FanOutShape2}
import akka.stream.stage.{AsyncCallback, GraphStage}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.xmpp.{XmppMessageIdHelper, XmppGatewayCache}
import com.flipkart.connekt.commons.iomodels._
import akka.stream._
import akka.stream.stage._
import com.flipkart.connekt.commons.services.ConnektConfig
import scala.util.{Success, Try}

/**
 * Created by subir.dey on 22/06/16.
 */
class GcmXmppDispatcher extends GraphStage[FanOutShape2[(GcmXmppRequest,GCMRequestTracker), (Try[XmppResponse], GCMRequestTracker), XmppUpstreamData]] {

  val maxPendingUpstreamCount = ConnektConfig.get("gcm.xmpp.appIds").getOrElse("100").toInt
  val in = Inlet[(GcmXmppRequest,GCMRequestTracker)]("GcmXmppDispatcher.In")
  val outDownstream = Outlet[(Try[XmppResponse], GCMRequestTracker)]("GcmXmppDispatcher.outDownstream")
  val outUpstream = Outlet[XmppUpstreamData]("GcmXmppDispatcher.outUpstream")
  override def shape = new FanOutShape2[(GcmXmppRequest,GCMRequestTracker), (Try[XmppResponse], GCMRequestTracker), XmppUpstreamData](in, outDownstream, outUpstream)

  //to ask for more from in
  var getMoreCallback: AsyncCallback[String] = null

  //to push downstream
  var ackRecvdCallback: AsyncCallback[(Try[XmppResponse], GCMRequestTracker)] = null

  //to push upstream data
  var upStreamRecvdCallback: AsyncCallback[(ActorRef,XmppUpstreamData)] = null

  var drRecvdCallback: AsyncCallback[(ActorRef,XmppReceipt)] = null

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
  val xmppState = new XmppGatewayCache

    override def preStart(): Unit = {
      getMoreCallback = getAsyncCallback[String] {
        appId => {
          xmppState.incrementConnectionFreeCount(appId)
          if ( !hasBeenPulled(in) )
            pull(in)
        }
      }

      ackRecvdCallback = getAsyncCallback[(Try[XmppResponse], GCMRequestTracker)] {
        downstreamResponse => {
          if ( isAvailable(outDownstream))
            push(outDownstream, downstreamResponse)
          else
            xmppState.enqueuDownstream(downstreamResponse)
        }
      }

      drRecvdCallback = getAsyncCallback[(ActorRef,XmppReceipt)] {
        drResponse => {
          if ( isAvailable(outDownstream) ) {
            val deliveryReceipt:XmppReceipt = drResponse._2
            drResponse._1 ! deliveryReceipt
            val parsedMessage = XmppMessageIdHelper.parseMessageIdTo(deliveryReceipt.data.originalMessageId)
            push(outDownstream, (Success(deliveryReceipt),
                                          GCMRequestTracker(messageId = parsedMessage._1,
                                          clientId = deliveryReceipt.from,
                                          deviceId = Seq(deliveryReceipt.data.deviceRegistrationId),
                                          appName = deliveryReceipt.category,
                                          contextId = parsedMessage._2,
                                          meta = Map())))
          }
          else
            xmppState.enqueuDR(drResponse)
        }
      }

      upStreamRecvdCallback = getAsyncCallback[(ActorRef,XmppUpstreamData)] {
        upstreamResponse => {
          if ( isAvailable(outUpstream) ) {
            upstreamResponse._1 ! upstreamResponse._2
            push(outUpstream, upstreamResponse._2)
          }
          else
            xmppState.enqueuUpstream(upstreamResponse)
        }
      }
    }

    val inhandler = new InHandler {
      override def onPush(): Unit = {
        val input:GCMPayloadEnvelope = grab(in)
        val requestPair = mapXmppRequest(input)
        xmppState.addIncomingRequest(requestPair)

        //exhaust buffer as long as connections available
        xmppState.sendRequests

        //pull more if connections available
        if ( (xmppState.responsesUpStream.size + xmppState.responsesDR.size) < maxPendingUpstreamCount && xmppState.connectionAvailable > 0 )
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
        if (xmppState.responsesDR.nonEmpty ) {
          val drResponse = xmppState.responsesDR.dequeue()
          val deliveryReceipt:XmppReceipt = drResponse._2

          drResponse._1 ! deliveryReceipt

          val parsedMessage = XmppMessageIdHelper.parseMessageIdTo(deliveryReceipt.data.originalMessageId)
          push(outDownstream, (Success(deliveryReceipt),
            GCMRequestTracker(messageId = parsedMessage._1,
              clientId = deliveryReceipt.from,
              deviceId = Seq(deliveryReceipt.data.deviceRegistrationId),
              appName = deliveryReceipt.category,
              contextId = parsedMessage._2,
              meta = Map())))
        }
        else if ( xmppState.responsesDownStream.nonEmpty )
          push(outDownstream, xmppState.responsesDownStream.dequeue())

        if ( (xmppState.responsesUpStream.size  + xmppState.responsesDR.size) < maxPendingUpstreamCount && xmppState.connectionAvailable > 0 && !hasBeenPulled(in))
          pull(in)
      }
    }
    setHandler(outDownstream, downstreamOutHandler)

    val upstreamUphandler = new OutHandler {
      override def onPull(): Unit = {
        if (xmppState.responsesUpStream.nonEmpty) {
          val upstreamResponse = xmppState.responsesUpStream.dequeue()
          upstreamResponse._1 ! upstreamResponse._2
          push(outUpstream, upstreamResponse._2)
        }
        if ((xmppState.responsesUpStream.size + xmppState.responsesDR.size) < maxPendingUpstreamCount && xmppState.connectionAvailable > 0 && !hasBeenPulled(in))
          pull(in)
      }
    }
    setHandler(outUpstream, upstreamUphandler)
  }
}
