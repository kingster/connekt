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

import akka.actor.{ActorRef, Terminated, Props, Actor}
import akka.routing.{ActorRefRoutee, Router, RoundRobinRoutingLogic}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.{ConnectionBusy, XmppRequestAvailable, FreeConnectionAvailable}
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.GcmXmppRequest
import com.flipkart.connekt.commons.services.ConnektConfig
import scala.collection.mutable
class XmppConnectionRouter (dispatcher: GcmXmppDispatcher, googleCredential: GoogleCredential, appId:String) extends Actor {
  val requests:mutable.Queue[(GcmXmppRequest, GCMRequestTracker)] = collection.mutable.Queue[(GcmXmppRequest, GCMRequestTracker)]()

  //TODO will be changed with zookeeper
  val connectionPoolSize = ConnektConfig.getInt("gcm.xmpp." + appId + ".count").getOrElse(3)
  val freeXmppActors = collection.mutable.LinkedHashSet[ActorRef]()

  var router:Router = {
    val routees = Vector.fill(connectionPoolSize) {
      val aRoutee = context.actorOf(Props(classOf[XmppConnectionActor], dispatcher, googleCredential, appId)
        .withMailbox("akka.actor.xmpp-connection-priority-mailbox"))
      context watch aRoutee
      freeXmppActors.add(aRoutee)
      ActorRefRoutee(aRoutee)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val newRoutee = context.actorOf(Props(classOf[XmppConnectionActor], dispatcher, googleCredential, appId))
      context watch newRoutee
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

    case xmppRequest:(GcmXmppRequest, GCMRequestTracker) =>
      freeXmppActors.headOption match {
        case Some(worker:ActorRef) =>
          freeXmppActors.remove(worker)
          worker ! xmppRequest
        case _ =>
          //this case should never arise because connection actor pulls only when they are free
          ConnektLogger(LogFile.CLIENTS).error(s"Router asking for free worker ${requests.size} should never arise")
          requests.enqueue(xmppRequest)
          router.routees.foreach(r => r.send(XmppRequestAvailable, self))
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"xmppRequest:Request size ${requests.size} and free worker size ${freeXmppActors.size}")
  }
}
