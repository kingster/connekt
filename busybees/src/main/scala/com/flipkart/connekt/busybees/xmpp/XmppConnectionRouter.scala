package com.flipkart.connekt.busybees.xmpp

import akka.actor.{Terminated, Props, Actor}
import akka.routing.{ActorRefRoutee, Router, RoundRobinRoutingLogic}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor._
import com.flipkart.connekt.commons.iomodels.GCMXmppPNPayload
import com.flipkart.connekt.commons.services.ConnektConfig
import scala.collection.mutable
/**
 * Created by subir.dey on 22/06/16.
 */
class XmppConnectionRouter (dispatcher: GcmXmppDispatcher, appId:String) extends Actor {
  val requests:mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)] = collection.mutable.Queue[(GCMXmppPNPayload, GCMRequestTracker)]()
  val connectionPoolSize = ConnektConfig.getInt("gcm.xmpp." + appId + ".count").getOrElse(100)

  var router:Router = {
    val routees = Vector.fill(connectionPoolSize) {
      val aRoutee = context.actorOf(Props(classOf[XmppConnectionActor], dispatcher, appId)
        .withMailbox("akka.actor.xmpp-connection-priority-mailbox"))
      context watch aRoutee
      ActorRefRoutee(aRoutee)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case InitXmpp => router.routees.foreach(r => r.send(InitXmpp, self))

    case Terminated(a) =>
      router = router.removeRoutee(a)
      val newRoutee = context.actorOf(Props(classOf[XmppConnectionActor], dispatcher, appId))
      context watch newRoutee
      router = router.addRoutee(newRoutee)

    case FreeConnectionAvailable =>
      if ( requests.nonEmpty )
        sender ! requests.dequeue()

    case xmppRequest:(GCMXmppPNPayload, GCMRequestTracker) =>
      requests.enqueue(xmppRequest)
      router.routees.foreach(r => r.send(XmppRequestAvailable, self))
  }
}
