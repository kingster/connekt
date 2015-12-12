package com.flipkart.connekt.busybees.clients

import akka.actor.Actor
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._
import org.apache.commons.lang.StringUtils
import org.jivesoftware.smack.filter.StanzaTypeFilter
import org.jivesoftware.smack.packet._
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.jivesoftware.smack.{ReconnectionManager, ConnectionListener, StanzaListener, XMPPConnection}

import scala.xml.XML

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
class XMPPChannelHandler private (xmppConnection: XMPPTCPConnection)
                                 (downStreamHandler: XmppRequest => Unit, upStreamHandler: XmppResponse => Unit) extends Actor {

  private val gcmMessageElement = "gcm"
  private val gcmMessageNamespace = "google:mobile:data"

  private var username: String = StringUtils.EMPTY
  private var password: String = StringUtils.EMPTY

  var connection: XMPPTCPConnection = xmppConnection

  def this(xmppHost: String, xmppPort: Int, username: String, password: String)
          (downStreamHandler: XmppRequest => Unit, upStreamHandler: XmppResponse => Unit) = {

    this(new XMPPTCPConnection(XMPPTCPConnectionConfiguration.builder()
      .setHost(xmppHost).setPort(xmppPort)
      .setUsernameAndPassword(username, password)
      .setCompressionEnabled(true).build()
    ))(downStreamHandler, upStreamHandler)

    this.username = username
    this.password = password
  }

  def configure() = {
    ConnektLogger(LogFile.CLIENTS).debug(s"Configuring XMPPChannelHandler: ${self.path}")
    connection.addSyncStanzaListener(new StanzaListener {
      override def processPacket(packet: Stanza): Unit = {
        val m = packet.asInstanceOf[Message]
        val x = m.getExtension(gcmMessageElement, gcmMessageNamespace).asInstanceOf[Element].toXML.toString
        val json = XML.loadString(x).text
        self ! json.getObj[XmppResponse]
      }
    }, new StanzaTypeFilter(classOf[Stanza]))

    connection.addConnectionListener(new ConnectionListener {
      override def connected(connection: XMPPConnection): Unit = {
        ConnektLogger(LogFile.CLIENTS).debug(s"XMPPConnectionListener -> connected: ${connection.getStreamId}")
      }

      override def reconnectionFailed(e: Exception): Unit = {
        ConnektLogger(LogFile.CLIENTS).error(s"XMPPConnectionListener -> reConnectionFailure: ${e.getMessage}", e)
        self ! XMPPConnectionClosed
      }

      override def reconnectionSuccessful(): Unit = {
        ConnektLogger(LogFile.CLIENTS).debug(s"XMPPConnectionListener -> reConnectionSuccess")
      }

      override def authenticated(connection: XMPPConnection, resumed: Boolean): Unit = {
        ConnektLogger(LogFile.CLIENTS).info(s"XMPPConnectionListener -> authenticated: ${connection.getStreamId}")
      }

      override def connectionClosedOnError(e: Exception): Unit = {
        ConnektLogger(LogFile.CLIENTS).error(s"XMPPConnectionListener -> connectionClosedOnError: ${e.getMessage}", e)
      }

      override def connectionClosed(): Unit = {
        ConnektLogger(LogFile.CLIENTS).error(s"XMPPConnectionListener -> connectionClosed")
        self ! XMPPConnectionClosed
      }

      override def reconnectingIn(seconds: Int): Unit = {
        ConnektLogger(LogFile.CLIENTS).debug(s"XMPPConnectionListener -> reConnectingIn: $seconds s")
      }
    })

    ReconnectionManager.getInstanceFor(connection).enableAutomaticReconnection()
    connection.connect()
    connection.login(username, password)
  }

  override def receive: Receive = {
    case c: Configure =>
      configure()
    case r: XmppRequest =>
      connection.sendStanza(new GCMStanza(r.messageId, r.getJson))
    case control: XmppControl =>
      if(control.controlType.equalsIgnoreCase("CONNECTION_DRAINING")) {
        context.become(drainingReceiver)
        context.parent ! CreateNewXMPPChannelHandler
      }
    case m: XmppResponse =>
      upStreamHandler(m)
    case XMPPConnectionClosed =>
      context.stop(self)
  }

  def drainingReceiver: Receive = {
    case xmppRequest: XmppRequest =>
      sender() ! xmppRequest
    case m: XmppResponse =>
      upStreamHandler(m)
    case XMPPConnectionClosed =>
      context.stop(self)
  }

  private class GCMStanza(messageId: String, json: String) extends Stanza {

    override def toXML: CharSequence =
      s"""
       |<message id=\"$messageId\">
       |  <$gcmMessageElement xmlns=\"$gcmMessageNamespace\">
       |    $json
       |  </gcm>
       |</message>
     """.stripMargin
  }
}

case class XMPPConnectionClosed()
case class Configure()
