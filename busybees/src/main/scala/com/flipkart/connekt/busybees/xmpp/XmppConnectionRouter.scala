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

import akka.actor._
import akka.routing.{ActorRefRoutee, Router, RoundRobinRoutingLogic}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.{ConnectionBusy, XmppRequestAvailable, FreeConnectionAvailable, Shutdown, StartShuttingDown}
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.GcmXmppRequest
import com.flipkart.connekt.commons.services.ConnektConfig
import scala.collection.mutable

class XmppConnectionRouter (dispatcher: GcmXmppDispatcher, googleCredential: GoogleCredential, appId:String) extends Actor {
  val requests:mutable.Queue[XmppOutStreamRequest] = collection.mutable.Queue[XmppOutStreamRequest]()

  //TODO will be changed with zookeeper
  val connectionPoolSize = ConnektConfig.getInt("gcm.xmpp." + appId + ".count").getOrElse(10)
  val freeXmppActors = collection.mutable.LinkedHashSet[ActorRef]()

  override def postStop = {
    ConnektLogger(LogFile.CLIENTS).info("XmppConnectionRouter:In postStop")
  }

  var router:Router = {
    val routees = Vector.fill(connectionPoolSize) {
      val aRoutee = context.actorOf(Props(classOf[XmppConnectionActor], dispatcher, googleCredential, appId)
        .withMailbox("akka.actor.xmpp-connection-priority-mailbox"))
      context.watch(aRoutee)
      freeXmppActors.add(aRoutee)
      ActorRefRoutee(aRoutee)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  import context._

  def receive = {
    case Terminated(a) =>
      ConnektLogger(LogFile.CLIENTS).trace(s"Worker terminated $a")
      router = router.removeRoutee(a)
      val newRoutee = context.actorOf(Props(classOf[XmppConnectionActor], dispatcher, googleCredential, appId))
      context.watch(newRoutee)
      router = router.addRoutee(newRoutee)


    case FreeConnectionAvailable =>
      if ( requests.nonEmpty )
        sender ! requests.dequeue()
      else {
        if ( freeXmppActors.add(sender) )
          dispatcher.getMoreCallback.invoke(appId)
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"FreeConnectionAvailable:Request size ${requests.size} and free worker size ${freeXmppActors.size}")

    case ConnectionBusy =>
      if ( freeXmppActors.nonEmpty )
        dispatcher.getMoreCallback.invoke(appId)
      ConnektLogger(LogFile.CLIENTS).trace(s"ConnectionBusy:Request size ${requests.size} and free worker size ${freeXmppActors.size}")

    case xmppRequest:XmppOutStreamRequest =>
      freeXmppActors.headOption match {
        case Some(worker:ActorRef) =>
          freeXmppActors.remove(worker)
          worker ! xmppRequest
        case _ =>
          //this case should never arise because connection actor pulls only when they are free
          ConnektLogger(LogFile.CLIENTS).error(s"Router asking for free worker ${requests.size} should never arise")
          requests.enqueue(xmppRequest)
          router.routees.foreach(_.send(XmppRequestAvailable, self))
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"xmppRequest:Request size ${requests.size} and free worker size ${freeXmppActors.size}")

    case Shutdown =>
      ConnektLogger(LogFile.CLIENTS).info(s"Shutdown received size ${requests.size} and free worker size ${freeXmppActors.size}")
      router.routees.foreach(_.send(Shutdown, self))
      become(shuttingDown)
  }

  def shuttingDown:Actor.Receive = {
    case Terminated(a) =>
      ConnektLogger(LogFile.CLIENTS).info(s"ShuttingDown:Worker terminated $a")
      router = router.removeRoutee(a)
      if ( router.routees.isEmpty ) {
        ConnektLogger(LogFile.CLIENTS).info("ShuttingDown:All Worker terminated")
        context.stop(self)
      }
  }
}
