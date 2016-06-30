package com.flipkart.connekt.busybees.xmpp

import akka.actor.{ActorSystem, Props, ActorRef}
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

class XmppGatewayCache(implicit actorSystem:ActorSystem) {

  var connectionAvailable = 0
  val xmppRequestRouters:mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
  val requestBuffer:collection.mutable.Map[String,mutable.Queue[(GcmXmppRequest, GCMRequestTracker)]] = collection.mutable.Map()
  val connectionFreeCount:collection.mutable.Map[String,Int] = collection.mutable.Map()
  val responsesDownStream:mutable.Queue[(Try[XmppDownstreamResponse], GCMRequestTracker)] = collection.mutable.Queue[(Try[XmppDownstreamResponse], GCMRequestTracker)]()
  val responsesUpStream:mutable.Queue[(ActorRef,XmppUpstreamResponse)] = collection.mutable.Queue[(ActorRef,XmppUpstreamResponse)]()


  def xmppRequestRouter(appId:String):ActorRef = {
    xmppRequestRouters.get(appId) match {
      case Some(actorRef:ActorRef) => actorRef
      case None => {
        val xmppRequestRouter:ActorRef = actorSystem.actorOf(Props(classOf[XmppConnectionRouter], this, appId))
        xmppRequestRouters.put(appId, xmppRequestRouter)
        requestBuffer.put(appId,new mutable.Queue[(GcmXmppRequest, GCMRequestTracker)]())
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

  def addIncomingRequest(requestPair:(GcmXmppRequest, GCMRequestTracker)) = {
    val requests:mutable.Queue[(GcmXmppRequest, GCMRequestTracker)] = requestBuffer.get(requestPair._2.appName).get
    requests.enqueue(requestPair)
    requestBuffer.put(requestPair._2.appName, requests)
  }

  def incrementConnectionFreeCount(appId:String) = {
    connectionAvailable = connectionAvailable + 1
    val available:Int = connectionFreeCount.get(appId).get + 1
    connectionFreeCount.put(appId, available)
  }

  def enqueueDownstream(downstreamResponse:(Try[XmppDownstreamResponse], GCMRequestTracker)) = {
    responsesDownStream.enqueue(downstreamResponse)
  }

  def dequeueDownstream:(Try[XmppDownstreamResponse], GCMRequestTracker) = {
    responsesDownStream.dequeue()
  }

  def enqueueUpstream(upstreamResponse:(ActorRef,XmppUpstreamResponse)) = {
    responsesUpStream.enqueue(upstreamResponse)
  }

  def dequeueUpstream:(ActorRef,XmppUpstreamResponse) = {
    responsesUpStream.dequeue()
  }
}
