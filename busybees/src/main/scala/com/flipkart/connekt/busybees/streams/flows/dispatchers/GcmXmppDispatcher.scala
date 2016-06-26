package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.actor.{Props, ActorRef}
import akka.stream.{Inlet, Outlet, FanOutShape2}
import akka.stream.stage.{AsyncCallback, GraphStage}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.InitXmpp
import com.flipkart.connekt.busybees.xmpp.{XmppConnectionActor, XmppConnectionRouter}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._
import akka.stream._
import akka.stream.stage._
import com.flipkart.connekt.commons.services.ConnektConfig
import scala.collection.mutable
import scala.util.Try

/**
 * Created by subir.dey on 22/06/16.
 */
class GcmXmppDispatcher extends GraphStage[FanOutShape2[GCMPayloadEnvelope, (Try[XmppResponse], GCMRequestTracker), Either[XmppUpstreamData, XmppReceipt]]] {

  val in = Inlet[GCMPayloadEnvelope]("GcmXmppDispatcher.In")
  val outDownstream = Outlet[(Try[XmppResponse], GCMRequestTracker)]("GcmXmppDispatcher.outDownstream")
  val outUpstream = Outlet[Either[XmppUpstreamData, XmppReceipt]]("GcmXmppDispatcher.outUpstream")
  override def shape = new FanOutShape2[GCMPayloadEnvelope, (Try[XmppResponse], GCMRequestTracker), Either[XmppUpstreamData, XmppReceipt]](in, outDownstream, outUpstream)

  val appIdsSupprted:Array[String] = ConnektConfig.getString("gcm.xmpp.appIds").getOrElse("retailBroadcast").split(",")
  val xmppRequestRouters:Map[String, ActorRef] =
                          appIdsSupprted.map{
                            appId => {
                              val xmppRequestRouter:ActorRef = XmppConnectionActor.system.actorOf(Props(classOf[XmppConnectionRouter], this, appId))
                              (appId, xmppRequestRouter)
                            }
                          }.toMap

  //to ask for more from in
  var getMoreCallback: AsyncCallback[String] = null

  //to push downstream
  var ackRecvdCallback: AsyncCallback[(Try[XmppResponse], GCMRequestTracker)] = null

  //to push upstream
  var upStreamRecvdCallback: AsyncCallback[Either[XmppUpstreamData, XmppReceipt]] = null

  def  mapXmppRequest(payload:GCMPayloadEnvelope):(GCMXmppPNPayload, GCMRequestTracker) = {
    val xmppRequest:GCMXmppPNPayload = payload.gcmPayload.asInstanceOf[GCMXmppPNPayload]
    val requestTrace = GCMRequestTracker(payload.messageId, payload.clientId, payload.deviceId, payload.appName, payload.contextId, payload.meta)
    (xmppRequest, requestTrace)
  }

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    val responsesDownStream:mutable.Queue[(Try[XmppResponse], GCMRequestTracker)] = collection.mutable.Queue[(Try[XmppResponse], GCMRequestTracker)]()
    val responsesUpStream:mutable.Queue[Either[XmppUpstreamData, XmppReceipt]] = collection.mutable.Queue[Either[XmppUpstreamData, XmppReceipt]]()
    val requestBuffer:collection.mutable.Map[String,mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)]]
            = collection.mutable.Map() ++ appIdsSupprted.map{appId => (appId,new mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)]())}.toMap
    val connectionFreeCount:collection.mutable.Map[String,Int] = collection.mutable.Map() ++ appIdsSupprted.map{appId => (appId,0)}.toMap
    var connectionAvailable = 0

    def addRequest(requestPair:(GCMXmppPNPayload, GCMRequestTracker)) = {
      val requests:mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)] = requestBuffer.get(requestPair._2.appName).get
      requests.enqueue(requestPair)
      requestBuffer.put(requestPair._2.appName, requests)
    }

    override def preStart(): Unit = {
      getMoreCallback = getAsyncCallback[String] {
        appId => {
          connectionAvailable = connectionAvailable + 1
          val available:Int = connectionFreeCount.get(appId).get + 1
          connectionFreeCount.put(appId, available)
          if ( !hasBeenPulled(in) )
            pull(in)
        }
      }

      ackRecvdCallback = getAsyncCallback[(Try[XmppResponse], GCMRequestTracker)] {
        downstreamResponse => {
          if ( isAvailable(outDownstream))
            push(outDownstream, downstreamResponse)
          else
            responsesDownStream.enqueue(downstreamResponse)
        }
      }

      upStreamRecvdCallback = getAsyncCallback[Either[XmppUpstreamData, XmppReceipt]] {
        upstreamResponse => {
          if ( isAvailable(outUpstream) )
            push(outUpstream, upstreamResponse)
          else
            responsesUpStream.enqueue(upstreamResponse)
        }
      }

      xmppRequestRouters.foreach{ case (appId,router) => router ! InitXmpp}
    }

    val inhandler = new InHandler {
      override def onPush(): Unit = {
        val input:GCMPayloadEnvelope = grab(in)
        val requestPair = mapXmppRequest(input)
        addRequest(requestPair)

        //exhaust buffer as long as connections available
        requestBuffer.map{ case (appId, requests) =>
            var freeCount:Int = connectionFreeCount.get(appId).get
            while ( requests.nonEmpty && freeCount > 0 ) {
              connectionAvailable = connectionAvailable - 1
              freeCount = freeCount - 1
              xmppRequestRouters.get(appId).get ! requests.dequeue()
            }
            connectionFreeCount.put(appId, freeCount)
        }

        //pull more if connections available
        if ( connectionAvailable > 0 )
          pull(in)

        if ( responsesDownStream.nonEmpty && isAvailable(outDownstream))
          push(outDownstream, responsesDownStream.dequeue())

        if ( responsesUpStream.nonEmpty && isAvailable(outUpstream))
          push(outUpstream, responsesUpStream.dequeue())
      }

      override def onUpstreamFinish(): Unit = {
        //TODO NEED ALERT HERE
        //("INPUT STREAM IS CLOSED")
      }
    }
    setHandler(in, inhandler)

    val downstreamOutHandler = new OutHandler {
      override def onPull(): Unit = {
        if ( responsesDownStream.nonEmpty )
          push(outDownstream, responsesDownStream.dequeue())

        if ( connectionAvailable > 0 && !hasBeenPulled(in))
          pull(in)
      }
    }
    setHandler(outDownstream, downstreamOutHandler)

    val upstreamUphandler = new OutHandler {
      override def onPull(): Unit = {
        if ( responsesUpStream.nonEmpty )
          push(outUpstream, responsesUpStream.dequeue())
      }
      //DO NOT PULL. PULL is created from downstream
    }
    setHandler(outUpstream, upstreamUphandler)
  }
}
