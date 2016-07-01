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
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.{XmppRequestAvailable, FreeConnectionAvailable, InitXmpp}
import com.flipkart.connekt.commons.iomodels.GcmXmppRequest
import com.flipkart.connekt.commons.services.ConnektConfig
import scala.collection.mutable
/**
 * Created by subir.dey on 22/06/16.
 */
class XmppConnectionRouter (dispatcher: GcmXmppDispatcher, appId:String) extends Actor {
  val requests:mutable.Queue[(GcmXmppRequest, GCMRequestTracker)] = collection.mutable.Queue[(GcmXmppRequest, GCMRequestTracker)]()

  //TODO will be changed with zookeeper
  val connectionPoolSize = ConnektConfig.getInt("gcm.xmpp." + appId + ".count").getOrElse(1)
  val freeXmppActors = collection.mutable.Queue[ActorRef]()

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
      else
        freeXmppActors.enqueue(sender)

    case xmppRequest:(GcmXmppRequest, GCMRequestTracker) =>
      if ( freeXmppActors.nonEmpty ) {
        freeXmppActors.dequeue() ! xmppRequest
      }
      else {
        //this case should never arise because connection actor pulls only when they are free
        requests.enqueue(xmppRequest)
        router.routees.foreach(r => r.send(XmppRequestAvailable, self))
      }
  }
}
