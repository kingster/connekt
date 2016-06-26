package com.flipkart.connekt.busybees.xmpp

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory

import akka.actor.{ActorRef, Actor, ActorSystem}
import akka.dispatch.{UnboundedPriorityMailbox, PriorityGenerator}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor._
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig
import com.google.common.cache._
import com.typesafe.config.Config
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.provider.{ProviderManager, ExtensionElementProvider}
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.{SmackException, XMPPException, ConnectionConfiguration, XMPPConnection}
import org.jivesoftware.smack.tcp.{XMPPTCPConnectionConfiguration, XMPPTCPConnection}
import org.xmlpull.v1.XmlPullParser

import scala.util.{Failure, Success}

/**
 * Created by subir.dey on 22/06/16.
 */
object XmppConnectionActor {
  implicit val system = ActorSystem("xmpp-connection-system")
  case object InitXmpp
  case object ReConnect
  case object ConnectionDraining
  case object FreeConnectionAvailable
  case object XmppRequestAvailable
  case class ConnectionClosed(connection: XMPPTCPConnection)
  case class XmppAckExpired(tracker: GCMRequestTracker)

  val GCM_NAMESPACE:String = "google:mobile:data"
  val GCM_ELEMENT_NAME:String = "gcm"

  val jacksonModules = Seq(DefaultScalaModule)
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModules(jacksonModules: _*)
  mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)

  val archivedConnections: java.util.Set[XMPPConnection] = new java.util.HashSet[XMPPConnection]()

  lazy val xmppHost: String = ConnektConfig.get("gcm.ccs.gcmServer").getOrElse("gcm.googleapis.com")
  lazy val xmppPort: Int = ConnektConfig.get("gcm.ccs.gcmPort").getOrElse("5235").toInt
}

class XmppNeverAckException(message: String) extends Exception(message)
class XmppNackException(val response: XmppNack) extends Exception(response.errorDescription)

class XmppConnectionPriorityMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(
  PriorityGenerator {
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.ConnectionDraining => 0
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.ReConnect => 1
    case com.flipkart.connekt.commons.iomodels.XmppAck => 2
    case com.flipkart.connekt.commons.iomodels.XmppNack => 3
    case _ => 2
  })

class XmppConnectionActor(dispatcher: GcmXmppDispatcher, appId:String) extends Actor {
  val maxPendingAckCount = ConnektConfig.getInt("gcm.xmpp.maxcount").getOrElse(100)
  lazy val username: String = ConnektConfig.get("gcm.ccs." + appId + ".username").get
  lazy val apiKey: String = ConnektConfig.get("gcm.ccs." + appId + ".apiKey").get

  private var removalListener: RemovalListener[String, GCMRequestTracker] =
    new RemovalListener[String, GCMRequestTracker]() {
    def onRemoval(removal: RemovalNotification[String, GCMRequestTracker]) {
      if (removal.getCause != RemovalCause.EXPLICIT) {
        val messageData:GCMRequestTracker = removal.getValue
        self ! XmppAckExpired(messageData)
      }
    }
  }

  private val messageDataCache: Cache[String, GCMRequestTracker] = CacheBuilder.newBuilder()
    .expireAfterWrite(2, TimeUnit.MINUTES)
    .removalListener(removalListener)
    .maximumSize(4096)
    .build()

  var pendingAckCount:Int = 0
  var connection: XMPPTCPConnection = null

  @volatile protected var connectionDraining: Boolean = false

  import context._
  //message processing in the beginning
  def receive:Actor.Receive = {
    case InitXmpp =>
      //create pull demand in flow
      dispatcher.getMoreCallback.invoke(appId)
      parent ! FreeConnectionAvailable

    case xmppRequest:(GCMXmppPNPayload, GCMRequestTracker) =>
      //first request----create connection and process request
      createConnection()
      become(free)
      self ! xmppRequest

    case other:Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in init state s")
  }

  //message processing as long as pending ack is less than max
  def free:Actor.Receive = {
    case xmppRequest:(GCMXmppPNPayload, GCMRequestTracker) =>
      processSendRequest(parent, xmppRequest)

    case XmppRequestAvailable =>
      parent ! FreeConnectionAvailable

    case ack:XmppAck =>
      processAck(parent,ack)

    case nack:XmppNack =>
      processNack(parent,nack)

    case receipt:XmppReceipt =>
      processDeliveryReceipt(receipt)

    case upstreamMsg:XmppUpstreamData =>
      processUpstreamMessage(upstreamMsg)

    case ConnectionDraining =>
      processConnectionDraining

    case connClosed:ConnectionClosed =>
      processConnectionClosed(connClosed)

    case ackExpired:XmppAckExpired =>
      prcoessAckExpired(parent, ackExpired)
  }

  //message processing when pendingack has reached max
  def busy:Actor.Receive = {
    case XmppRequestAvailable => //do nothing

    case ack:XmppAck =>
      processAck(parent,ack)

    case nack:XmppNack =>
      processNack(parent,nack)

    case receipt:XmppReceipt =>
      processDeliveryReceipt(receipt)

    case upstreamMsg:XmppUpstreamData =>
      processUpstreamMessage(upstreamMsg)

    case ConnectionDraining =>
      processConnectionDraining

    case connClosed:ConnectionClosed =>
      processConnectionClosed(connClosed)

    case ackExpired:XmppAckExpired =>
      prcoessAckExpired(parent, ackExpired)
  }

  private def createConnection() = {
    connection = new XMPPTCPConnection(XMPPTCPConnectionConfiguration.builder()
                        .setHost(xmppHost)
                        .setPort(xmppPort)
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
                        .setSocketFactory(SSLSocketFactory.getDefault())
                        .setServiceName(appId)
                        .setSendPresence(false)
                        .build())

    ConnektLogger(LogFile.CLIENTS).debug(s"Configuring XMPPConnection")
    Roster.getInstanceFor(connection).setRosterLoadedAtLogin(false)
    connection.addConnectionListener(new ConnektXmppConnectionListener(connection, self))

    ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
      new ExtensionElementProvider[GcmXmppPacketExtension]() {
        override def parse(parser:XmlPullParser, initialDepth:Int):GcmXmppPacketExtension = {
          val json:String = parser.nextText()
          new GcmXmppPacketExtension(json)
        }
      }
    )

    val stanzaFilter = new StanzaFilter() {
      override def accept(stanza:Stanza):Boolean = if (stanza.hasExtension(GCM_ELEMENT_NAME, GCM_NAMESPACE)) true else false
    }
    val stanzaListener = new ConnektStanzaListener(self)
    connection.addAsyncStanzaListener(stanzaListener, stanzaFilter)

    try {
      connection.connect()
      connection.login(username, apiKey)
    }
    catch {
      case ex:SmackException | IOException | XMPPException =>
        //TODO how to handle
        ConnektLogger(LogFile.CLIENTS).error("Unable to connect:", ex)
    }
  }

  private def processSendRequest(parent:ActorRef, xmppRequest:(GCMXmppPNPayload, GCMRequestTracker)) = {
    pendingAckCount = pendingAckCount + 1
    val xmppPayload:GCMXmppPNPayload = xmppRequest._1
    val xmppPayloadString:String = XmppConnectionActor.mapper.writeValueAsString(xmppPayload)
    val packet = new GcmXmppPacketExtension(xmppPayloadString)
    val stanza:Stanza = new Stanza() {
      override def toXML():CharSequence = packet.toXML()
    }
    //TODO handle send failure
    connection.sendStanza(stanza)
    messageDataCache.put(xmppPayload.message_id,xmppRequest._2)

    if ( pendingAckCount >= maxPendingAckCount ) {
      become(busy)
      ConnektLogger(LogFile.CLIENTS).debug(s"XmppConnectionActor for ${appId} turned busy")
    } else {
      dispatcher.getMoreCallback.invoke(appId)
      parent ! FreeConnectionAvailable
    }
  }

  private def processAck(parent:ActorRef, ack:XmppAck) = {
    val xmppRequestTracker = messageDataCache.getIfPresent(ack.messageId)
    if ( xmppRequestTracker != null ) {
      messageDataCache.invalidate(ack.messageId)
      pendingAckCount = pendingAckCount - 1
      dispatcher.ackRecvdCallback.invoke((Success(ack), xmppRequestTracker))

      if ( pendingAckCount < maxPendingAckCount ) {
        become(free)
        dispatcher.getMoreCallback.invoke(appId)
        sender ! FreeConnectionAvailable
      }
    }
  }

  private def processNack(parent:ActorRef, ack:XmppNack) = {
    val xmppRequestTracker = messageDataCache.getIfPresent(ack.messageId)
    if ( xmppRequestTracker != null ) {
      messageDataCache.invalidate(ack.messageId)
      pendingAckCount = pendingAckCount - 1
      dispatcher.ackRecvdCallback.invoke((Failure(new XmppNackException(ack)), xmppRequestTracker))

      if ( pendingAckCount < maxPendingAckCount ) {
        become(free)
        dispatcher.getMoreCallback.invoke(appId)
        sender ! FreeConnectionAvailable
      }
    }
  }

  private def prcoessAckExpired(parent:ActorRef, ackExpired:XmppAckExpired) = {
    if ( ackExpired.tracker != null ) {
      pendingAckCount = pendingAckCount - 1
      dispatcher.ackRecvdCallback.invoke((Failure(new XmppNeverAckException("Ack never received. Cache timed out")), ackExpired.tracker))

      if ( pendingAckCount < maxPendingAckCount ) {
        become(free)
        dispatcher.getMoreCallback.invoke(appId)
        sender ! FreeConnectionAvailable
      }
    }
  }

  private def sendUpstreamAck(to: String, messageId: String) = {
    val ackMsg = XmppUpstreamAck(messageId, "ack", to)
    val xmppPayloadString:String = XmppConnectionActor.mapper.writeValueAsString(ackMsg)
    val packet = new GcmXmppPacketExtension(xmppPayloadString)
    val stanza:Stanza = new Stanza() {
      override def toXML():CharSequence = packet.toXML()
    }

    //TODO handle send failure
    connection.sendStanza(stanza)
  }

  private def processDeliveryReceipt(receipt:XmppReceipt) = {
    sendUpstreamAck(receipt.from, receipt.messageId)
    dispatcher.upStreamRecvdCallback.invoke(Right(receipt))
  }

  private def processUpstreamMessage(upstreamMsg:XmppUpstreamData) = {
    sendUpstreamAck(upstreamMsg.from, upstreamMsg.messageId)
    dispatcher.upStreamRecvdCallback.invoke(Left(upstreamMsg))
  }

  private def processConnectionDraining() = {
    ConnektLogger(LogFile.CLIENTS).error("Received ConnectionDraining!!:", appId)
    connectionDraining = true
    XmppConnectionActor.archivedConnections.add(connection)
    createConnection()
    connectionDraining = false

  }

  private def processConnectionClosed(connClosed:ConnectionClosed) = {
    ConnektLogger(LogFile.CLIENTS).error("Received Connectionclosed!!:", appId)
    XmppConnectionActor.archivedConnections.remove(connection)
  }
}
