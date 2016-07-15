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
package com.flipkart.connekt.busybees.xmpp

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Props, ActorRef}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig

import scala.collection.mutable
import scala.util.Try

/**
 * Note:
 * Works closely with GcmXmppDispatcher to maintain state
 * Since GraphStageLogic is synchronised, we don't need any synchronisation
 */

class XmppGatewayCache(parent:GcmXmppDispatcher)(implicit actorSystem:ActorSystem) {

  var connectionAvailable = 0
  val xmppRequestRouters:mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
  val requestBuffer:collection.mutable.Map[String,mutable.Queue[(GcmXmppRequest, GCMRequestTracker)]] = collection.mutable.Map()
  val connectionFreeCount:collection.mutable.Map[String,AtomicInteger] = collection.mutable.Map()
  val responsesDownStream:mutable.Queue[(Try[XmppDownstreamResponse], GCMRequestTracker)] = collection.mutable.Queue[(Try[XmppDownstreamResponse], GCMRequestTracker)]()
  val responsesUpStream:mutable.Queue[(ActorRef,XmppUpstreamResponse)] = collection.mutable.Queue[(ActorRef,XmppUpstreamResponse)]()

  private def initBuffer(appId:String, credential:GoogleCredential) = {
    //TODO will be changed with zookeeper
    val connectionPoolSize = ConnektConfig.getInt("gcm.xmpp." + appId + ".count").getOrElse(1)
    val xmppRequestRouter:ActorRef = actorSystem.actorOf(Props(classOf[XmppConnectionRouter], parent, credential, appId))
    xmppRequestRouters.put(appId, xmppRequestRouter)
    requestBuffer.put(appId,new mutable.Queue[(GcmXmppRequest, GCMRequestTracker)]())
    connectionFreeCount.put(appId,new AtomicInteger(connectionPoolSize))
    connectionAvailable = connectionAvailable + connectionPoolSize
  }

  def sendRequests() = {
    requestBuffer.foreach{ case (appId, requests) =>
      val freeCount:AtomicInteger = connectionFreeCount.get(appId).get
      while ( requests.nonEmpty && freeCount.get > 0 ) {
        connectionAvailable = connectionAvailable - 1
        freeCount.decrementAndGet()
        xmppRequestRouters.get(appId).get ! requests.dequeue()
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"in gateway cache request size ${requests.size}, connect count $connectionAvailable")
    }
  }

  def addIncomingRequest(requestPair:(GcmXmppRequest, GCMRequestTracker)) = {
    val requests:mutable.Queue[(GcmXmppRequest, GCMRequestTracker)] =
      requestBuffer.get(requestPair._2.appName).getOrElse({
        initBuffer(requestPair._2.appName, requestPair._1.credential)
        requestBuffer.get(requestPair._2.appName).get
      })
    requests.enqueue(requestPair)
    ConnektLogger(LogFile.CLIENTS).trace(s"in gateway cache request size ${requests.size}, connect count $connectionAvailable")
    requestBuffer.put(requestPair._2.appName, requests)
  }

  def incrementConnectionFreeCount(appId:String) = {
    connectionAvailable = connectionAvailable + 1
    connectionFreeCount.get(appId).get.incrementAndGet()
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
