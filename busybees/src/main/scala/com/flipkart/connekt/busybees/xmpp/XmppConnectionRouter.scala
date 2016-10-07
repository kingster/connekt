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
import akka.event.LoggingReceive
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import com.flipkart.connekt.busybees.xmpp.Internal.{ConnectionBusy, FreeConnectionAvailable, ReSize, XmppRequestAvailable}
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.{SendXmppOutStreamRequest, Shutdown}
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.collection.mutable

class XmppConnectionRouter (var connectionPoolSize:Int, googleCredential: GoogleCredential, appId:String,stageLogicRef :ActorRef) extends Actor with ActorLogging {

  val requests:mutable.Queue[SendXmppOutStreamRequest] = collection.mutable.Queue[SendXmppOutStreamRequest]()
  val freeXmppActors = collection.mutable.LinkedHashSet[ActorRef]()

  override def postStop = {
    ConnektLogger(LogFile.CLIENTS).info("XmppConnectionRouter:In postStop")
  }

  var router:Router = {
    val routees = Vector.fill(connectionPoolSize) {
      createRoutee()
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  private def createRoutee():ActorRefRoutee = {
    ConnektLogger(LogFile.CLIENTS).trace(s"Creating XmppConnectionActor for $appId")
    val aRoutee = context.actorOf(Props(classOf[XmppConnectionActor],  googleCredential, appId, stageLogicRef)
      .withMailbox("akka.actor.xmpp-connection-priority-mailbox"))
    context.watch(aRoutee)
    freeXmppActors.add(aRoutee)
    ActorRefRoutee(aRoutee)
  }

  import context._

  def receive = LoggingReceive {
    case Terminated(a) =>
      ConnektLogger(LogFile.CLIENTS).trace(s"XmppConnectionRouter: XmppConnectionActor Terminated $a")
      freeXmppActors.remove(a)
      router = router.removeRoutee(a)
      if(router.routees.size < connectionPoolSize)
        router = router.addRoutee(createRoutee())

    case ReSize(count) =>
      ConnektLogger(LogFile.CLIENTS).info(s"Will Resize XMPP Actor to $count for $appId")
      connectionPoolSize = count
      val currentCount = router.routees.size
      if (count < currentCount) {
        //Destroy few...
        ConnektLogger(LogFile.CLIENTS).debug(s"Resize XMPP Actor : Reduce")
        router.routees.slice(count+1 , currentCount).foreach(_.send(Shutdown, self))
      } else {
        //create some new
        ConnektLogger(LogFile.CLIENTS).debug(s"Resize XMPP Actor : Increase")
        for(i <- currentCount+1 to count) router = router.addRoutee(createRoutee())
      }

    case FreeConnectionAvailable =>
      if ( requests.nonEmpty )
        sender ! requests.dequeue()
      else {
        freeXmppActors.add(sender)
        stageLogicRef ! FreeConnectionAvailable // TODO ?? APPID?
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"FreeConnectionAvailable:Request size ${requests.size} and free worker size ${freeXmppActors.size}")

    case ConnectionBusy =>
      if ( freeXmppActors.nonEmpty ){
        stageLogicRef ! FreeConnectionAvailable
      }
      ConnektLogger(LogFile.CLIENTS).trace(s"ConnectionBusy:Request size ${requests.size} and free worker size ${freeXmppActors.size}")

    case xmppRequest:SendXmppOutStreamRequest =>
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

    case s:Shutdown =>
      ConnektLogger(LogFile.CLIENTS).info(s"Shutdown received size ${requests.size} and free worker size ${freeXmppActors.size}")
      become(shuttingDown)
      router.routees.foreach(_.send(s, self))
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

