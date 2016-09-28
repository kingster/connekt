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
import akka.pattern.ask
import akka.util.Timeout
import com.flipkart.connekt.busybees.discovery.DiscoveryManager
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.{FreeConnectionCount, ReSize}
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
/**
 * Note:
 * Works closely with GcmXmppDispatcher to maintain state
 * Since GraphStageLogic is synchronised, we don't need any synchronisation
 */

class XmppGatewayCache(parent:GcmXmppDispatcher)(implicit actorSystem:ActorSystem)  extends SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.DISCOVERY_CHANGE))

  val xmppRequestRouters:mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
  val requestBuffer:collection.mutable.Map[String,mutable.Queue[XmppOutStreamRequest]] = collection.mutable.Map()
  val connectionFreeCount:collection.mutable.Map[String,AtomicInteger] = collection.mutable.Map()
  val responsesDownStream:ConcurrentLinkedQueue[(Try[XmppDownstreamResponse], GCMRequestTracker)] = new ConcurrentLinkedQueue[(Try[XmppDownstreamResponse], GCMRequestTracker)]()
  val responsesUpStream:mutable.Queue[(ActorRef,XmppUpstreamResponse)] = collection.mutable.Queue[(ActorRef,XmppUpstreamResponse)]()
  val maxXmppConnections =  ConnektConfig.getInt("gcm.xmpp.maxConnections").getOrElse(50)

  /**
    * This is used by GateWayCache to back-pressure input ports
    * @return Int
    */
  def totalConnectionAvailable:Int = {
    connectionFreeCount.values.map(_.get()).sum
  }

  private def getPoolSize(clusterSize: Int): Int = {
    if( clusterSize > 0 )
      math.min(maxXmppConnections, maxXmppConnections / clusterSize)
    else
      maxXmppConnections
  }

  private def initBuffer(appId:String, credential:GoogleCredential) = {
    val currentClusterSize = DiscoveryManager.instance.getInstances.size
    val xmppRequestRouter:ActorRef = actorSystem.actorOf(Props(classOf[XmppConnectionRouter], getPoolSize(currentClusterSize), parent, credential, appId))
    xmppRequestRouters.put(appId, xmppRequestRouter)
    requestBuffer.put(appId,new mutable.Queue[XmppOutStreamRequest]())
    connectionFreeCount.put(appId,new AtomicInteger(maxXmppConnections))
  }

  def sendRequests() = {
    requestBuffer.foreach{ case (appId, requests) =>
      val freeCount = connectionFreeCount(appId)
      while ( requests.nonEmpty && freeCount.get > 0 ) { //needs fix here.
        freeCount.decrementAndGet()
        xmppRequestRouters(appId) ! requests.dequeue()
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"in gateway cache request size ${requests.size}, connect count $totalConnectionAvailable")
    }
  }

  def addIncomingRequest(request:XmppOutStreamRequest) = {
    val requests:mutable.Queue[XmppOutStreamRequest] =
      requestBuffer.getOrElse(request.tracker.appName, {
        initBuffer(request.tracker.appName, request.request.credential)
        requestBuffer(request.tracker.appName)
      })
    requests.enqueue(request)
    ConnektLogger(LogFile.CLIENTS).trace(s"in gateway cache request size ${requests.size}, connect count $totalConnectionAvailable")
    requestBuffer.put(request.tracker.appName, requests)
  }

  def incrementConnectionFreeCount(appId:String) = {
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
          appId -> gracefulStop(xmppRouter, 10.second, com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.Shutdown)
      }
      Await.result(Future.sequence(futures.values), 15.second)
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
    xmppRequestRouters.clear
    requestBuffer.clear
    connectionFreeCount.clear
    responsesDownStream.clear()
    responsesUpStream.clear
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Any = {
    implicit val duration: Timeout = 5.seconds

    _type match {
      case SyncType.DISCOVERY_CHANGE =>
        ConnektLogger(LogFile.SERVICE).error(s"Will Re-balance Router Size Now. New Length ${args.length}, Data : $args")
        if(args.nonEmpty ) {
          xmppRequestRouters.foreach {
            case (appId, xmppRouter) =>
              xmppRouter ! ReSize(getPoolSize(args.length))
              val count = xmppRequestRouters(appId) ? FreeConnectionCount
              count.onComplete {
                case Success(value: Int) =>
                  ConnektLogger(LogFile.SERVICE).info(s"Update connectionFreeCount from Router on ReBalance for $appId to $value")
                  connectionFreeCount(appId).set(value)
                case Failure(e) =>
                  ConnektLogger(LogFile.SERVICE).error(s"Failed to Get connectionFreeCount from Router on ReBalance", e)
              }(actorSystem.dispatcher)
          }
        }
      case _ =>
    }

  }
}
