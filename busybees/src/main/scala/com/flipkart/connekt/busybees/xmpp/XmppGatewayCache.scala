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

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, ActorSystem, Props}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.Try

/**
 * Note:
 * Works closely with GcmXmppDispatcher to maintain state
 * Since GraphStageLogic is synchronised, we don't need any synchronisation
 */

class XmppGatewayCache(parent:GcmXmppDispatcher)(implicit actorSystem:ActorSystem) {

  var connectionAvailable = 0 // What is the need for this counter?
  val xmppRequestRouters:mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
  val requestBuffer:collection.mutable.Map[String,mutable.Queue[XmppOutStreamRequest]] = collection.mutable.Map()
  val connectionFreeCount:collection.mutable.Map[String,AtomicInteger] = collection.mutable.Map()
  val responsesDownStream:ConcurrentLinkedQueue[(Try[XmppDownstreamResponse], GCMRequestTracker)] = new ConcurrentLinkedQueue[(Try[XmppDownstreamResponse], GCMRequestTracker)]()
  val responsesUpStream:mutable.Queue[(ActorRef,XmppUpstreamResponse)] = collection.mutable.Queue[(ActorRef,XmppUpstreamResponse)]()

  private def initBuffer(appId:String, credential:GoogleCredential) = {
    //TODO will be changed with zookeeper
    val connectionPoolSize = ConnektConfig.getInt("gcm.xmpp." + appId + ".count").getOrElse(1)
    val xmppRequestRouter:ActorRef = actorSystem.actorOf(Props(classOf[XmppConnectionRouter],connectionPoolSize, parent, credential, appId))
    xmppRequestRouters.put(appId, xmppRequestRouter)
    requestBuffer.put(appId,new mutable.Queue[XmppOutStreamRequest]())
    connectionFreeCount.put(appId,new AtomicInteger(connectionPoolSize))
    connectionAvailable += connectionPoolSize
  }

  def sendRequests() = {
    requestBuffer.foreach{ case (appId, requests) =>
      val freeCount = connectionFreeCount(appId)
      while ( requests.nonEmpty && freeCount.get > 0 ) {
        connectionAvailable -= 1
        freeCount.decrementAndGet()
        xmppRequestRouters(appId) ! requests.dequeue()
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"in gateway cache request size ${requests.size}, connect count $connectionAvailable")
    }
  }

  def addIncomingRequest(request:XmppOutStreamRequest) = {
    val requests:mutable.Queue[XmppOutStreamRequest] =
      requestBuffer.getOrElse(request.tracker.appName, {
        initBuffer(request.tracker.appName, request.request.credential)
        requestBuffer(request.tracker.appName)
      })
    requests.enqueue(request)
    ConnektLogger(LogFile.CLIENTS).trace(s"in gateway cache request size ${requests.size}, connect count $connectionAvailable")
    requestBuffer.put(request.tracker.appName, requests)
  }

  def incrementConnectionFreeCount(appId:String) = {
    connectionAvailable += 1
    connectionFreeCount(appId).incrementAndGet()
  }

  def enqueueDownstream(downstreamResponse:(Try[XmppDownstreamResponse], GCMRequestTracker)) = {
    responsesDownStream.add(downstreamResponse)
  }

  def dequeueDownstream:(Try[XmppDownstreamResponse], GCMRequestTracker) = {
    responsesDownStream.poll()
  }

  def enqueueUpstream(upstreamResponse:(ActorRef,XmppUpstreamResponse)) = {
    responsesUpStream.enqueue(upstreamResponse)
  }

  def dequeueUpstream:(ActorRef,XmppUpstreamResponse) = {
    responsesUpStream.dequeue()
  }

  import akka.pattern.gracefulStop

  import scala.concurrent.duration._

  def prepareShutdown = {
    implicit val ec = actorSystem.dispatcher
    try {
      val futures: mutable.Map[String, Future[Boolean]] = xmppRequestRouters.map {
        case (appId, xmppRouter) =>
          appId -> gracefulStop(xmppRouter, 10 second, com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.Shutdown)
      }
      Await.result(Future.sequence(futures.values), 15 second)
    } catch {
      case e:Throwable =>
        ConnektLogger(LogFile.CLIENTS).error("Timeout for gracefully shutting down xmpp actors. Forcing")
        xmppRequestRouters.foreach {
          case (appId, xmppRouter) =>
            actorSystem.stop(xmppRouter)
        }
    }
  }

  def reset():Unit = {
    connectionAvailable = 0
    xmppRequestRouters.clear
    requestBuffer.clear
    connectionFreeCount.clear
    responsesDownStream.clear()
    responsesUpStream.clear
  }
}
