/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.busybees.clients

import akka.actor.{Terminated, Props, Actor}
import akka.routing.{RoundRobinRoutingLogic, Router, ActorRefRoutee}
import com.flipkart.connekt.commons.iomodels.XmppRequest

class XMPPChannelSupervisor(maxConnections: Int) extends Actor {
  var router = {
    val xmppChannelHandlers = Vector.fill(maxConnections) {
      val r = context.actorOf(Props[XMPPChannelHandler])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), xmppChannelHandlers)
  }

  override def receive: Receive = {
    case d: XmppRequest =>
      router.route(d, self)

    case CreateNewXMPPChannelHandler =>
      val r = context.actorOf(Props[XMPPChannelHandler])
      r ! Configure
      context watch r
      router = router.addRoutee(r)

    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[XMPPChannelHandler])
      r ! Configure
      context watch r
      router = router.addRoutee(r)
  }
}

case object CreateNewXMPPChannelHandler
