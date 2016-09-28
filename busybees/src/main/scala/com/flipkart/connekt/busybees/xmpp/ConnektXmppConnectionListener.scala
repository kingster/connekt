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

import akka.actor.ActorRef
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.{ConnectionClosed, ReConnect}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.{ConnectionListener, XMPPConnection}

class ConnektXmppConnectionListener(conn:XMPPTCPConnection, actorRef: ActorRef) extends ConnectionListener {
  override def connected(connection: XMPPConnection): Unit = {
    ConnektLogger(LogFile.CLIENTS).debug(s"XMPPConnectionListener -> connected: ${conn.getStreamId} s")
  }

  override def authenticated(connection: XMPPConnection, resumed: Boolean): Unit = {
    ConnektLogger(LogFile.CLIENTS).info(s"XMPPConnectionListener -> authenticated: ${conn.getStreamId} s")
  }

  override def connectionClosed(): Unit = {
    ConnektLogger(LogFile.CLIENTS).error(s"XMPPConnectionListener -> connectionClosed ${conn.getStreamId} s")
    actorRef ! ConnectionClosed(conn)
  }

  override def connectionClosedOnError(e: Exception): Unit = {
    ConnektLogger(LogFile.CLIENTS).error(s"XMPPConnectionListener -> connectionClosedOnError: ${conn.getStreamId} ${e.getMessage} s", e)
  }

  override def reconnectionSuccessful(): Unit = {
    ConnektLogger(LogFile.CLIENTS).debug(s"XMPPConnectionListener -> reConnectionSuccess: ${conn.getStreamId} s")
  }

  override def reconnectingIn(seconds: Int): Unit = {
    ConnektLogger(LogFile.CLIENTS).debug(s"XMPPConnectionListener -> reConnectingIn: ${conn.getStreamId} $seconds s")
  }

  override def reconnectionFailed(e: Exception): Unit = {
    ConnektLogger(LogFile.CLIENTS).error(s"XMPPConnectionListener -> reConnectionFailure: ${conn.getStreamId} ${e.getMessage} s", e)
    actorRef ! ConnectionClosed(conn)
  }
}
