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

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.gracefulStop
import akka.stream.stage.GraphStageLogic.StageActor
import akka.util.Timeout
import com.flipkart.connekt.busybees.discovery.DiscoveryManager
import com.flipkart.connekt.busybees.xmpp.Internal.ReSize
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.{SendXmppOutStreamRequest, Shutdown}
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.XmppDownstreamResponse
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

private[xmpp] class XMPPGateway(stageActor: StageActor)(implicit actorSystem: ActorSystem) extends SyncDelegate{

  SyncManager.get().addObserver(this, List(SyncType.DISCOVERY_CHANGE))

  val defaultXMPPConnectionCount = ConnektConfig.getInt("fcm.xmpp.defaultConnections").getOrElse(30)
  final val MAX_GOOGLE_ALLOWED_CONNECTIONS = ConnektConfig.getInt("fcm.xmpp.maxConnections").getOrElse(100)

  val xmppRequestRouters: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  /**
    * Send a request through the corresponding pool. If the pool is not running, it will be started
    * automatically.
    *
    * @param request the request
    * @return the response
    */
  def apply(request: XmppOutStreamRequest): Future[XmppDownstreamResponse] = {
    val responsePromise = Promise[XmppDownstreamResponse]()
    val appId = request.request.credential.projectId
    xmppRequestRouters.getOrElseUpdate(appId, createRouter(appId, request.request.credential)) ! SendXmppOutStreamRequest(request.request, responsePromise)
    responsePromise.future
  }

  /**
    * Shutdown the corresponding pool and signal its termination. If the pool is not running or is
    * being shutting down, this does nothing,
    *
    * @return a Future completed when the pool has been shutdown.
    */
  def shutdown(): Unit = {
    implicit val ec = actorSystem.dispatcher
    try {
      val futures: mutable.Map[String, Future[Boolean]] = xmppRequestRouters.map {
        case (appId, xmppRouter) =>
          stageActor.unwatch(xmppRouter)
          val shutdownCompletedPromise = Promise[Done]()
          appId -> gracefulStop(xmppRouter, 10.second, Shutdown(discardPendingAcks = true, shutdownCompletedPromise))
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

  private def createRouter(appId: String, credential: GoogleCredential): ActorRef = {
    val currentClusterSize = DiscoveryManager.instance.getInstances.size
    val poolSize = getPoolSize(currentClusterSize)
    val router = actorSystem.actorOf(Props(classOf[XmppConnectionRouter], poolSize, credential, appId, stageActor.ref))
    stageActor.watch(router)
    router
  }

  private def getPoolSize(clusterSize: Int): Int = {
    if (clusterSize > 1)
      math.min(defaultXMPPConnectionCount, MAX_GOOGLE_ALLOWED_CONNECTIONS / clusterSize)
    else
      defaultXMPPConnectionCount
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
          }
        }
      case _ =>
    }

  }
}
