package com.flipkart.connekt.busybees.xmpp

import akka.actor.{Props, ActorRef}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.InitXmpp
import com.flipkart.connekt.commons.iomodels._

import scala.collection.mutable
import scala.util.Try

/**
 * Created by subir.dey on 28/06/16.
 * Note:
 * Works closely with GcmXmppDispatcher to maintain state
 * Since GraphStageLogic is synchronised, we don't need any synchronisation
 */

class XmppGatewayCache {

  var connectionAvailable = 0
  val xmppRequestRouters:mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
  val requestBuffer:collection.mutable.Map[String,mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)]] = collection.mutable.Map()
  val connectionFreeCount:collection.mutable.Map[String,Int] = collection.mutable.Map()
  val responsesDownStream:mutable.Queue[(Try[XmppResponse], GCMRequestTracker)] = collection.mutable.Queue[(Try[XmppResponse], GCMRequestTracker)]()
  val responsesUpStream:mutable.Queue[(ActorRef,XmppUpstreamData)] = collection.mutable.Queue[(ActorRef,XmppUpstreamData)]()
  val responsesDR:mutable.Queue[(ActorRef,XmppReceipt)] = collection.mutable.Queue[(ActorRef,XmppReceipt)]()


  def xmppRequestRouter(appId:String):ActorRef = {
    xmppRequestRouters.get(appId) match {
      case Some(actorRef:ActorRef) => actorRef
      case None => {
        val xmppRequestRouter:ActorRef = XmppConnectionHelper.system.actorOf(Props(classOf[XmppConnectionRouter], this, appId))
        xmppRequestRouters.put(appId, xmppRequestRouter)
        requestBuffer.put(appId,new mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)]())
        connectionFreeCount.put(appId,0)
        xmppRequestRouter ! InitXmpp
        xmppRequestRouter
      }
    }
  }

  def sendRequests = {
    requestBuffer.map{ case (appId, requests) =>
      var freeCount:Int = connectionFreeCount.get(appId).get
      while ( requests.nonEmpty && freeCount > 0 ) {
        connectionAvailable = connectionAvailable - 1
        freeCount = freeCount - 1
        xmppRequestRouter(appId) ! requests.dequeue()
      }
      connectionFreeCount.put(appId, freeCount)
    }
  }

  def addIncomingRequest(requestPair:(GCMXmppPNPayload, GCMRequestTracker)) = {
    val requests:mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)] = requestBuffer.get(requestPair._2.appName).get
    requests.enqueue(requestPair)
    requestBuffer.put(requestPair._2.appName, requests)
  }

  def incrementConnectionFreeCount(appId:String) = {
    connectionAvailable = connectionAvailable + 1
    val available:Int = connectionFreeCount.get(appId).get + 1
    connectionFreeCount.put(appId, available)
  }

  def enqueuDownstream(downstreamResponse:(Try[XmppResponse], GCMRequestTracker)) = {
    responsesDownStream.enqueue(downstreamResponse)
  }

  def enqueuUpstream(upstreamResponse:(ActorRef,XmppUpstreamData)) = {
    responsesUpStream.enqueue(upstreamResponse)
  }

  def enqueuDR(drResponse:(ActorRef,XmppReceipt)) = {
    responsesDR.enqueue(drResponse)
  }
}
