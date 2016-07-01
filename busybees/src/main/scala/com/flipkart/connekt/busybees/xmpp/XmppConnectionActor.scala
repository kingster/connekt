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

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory

import akka.actor.{ActorRef, Actor}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper._
import com.flipkart.connekt.commons.entities.{GoogleCredential, Credential}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils
import com.google.common.cache._
import org.jivesoftware.smack.SmackException.NotConnectedException
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

class XmppConnectionActor(dispatcher: GcmXmppDispatcher, appId:String) extends Actor {
  val maxPendingAckCount = ConnektConfig.getInt("gcm.xmpp.maxcount").getOrElse(100)
  var googleCredential:GoogleCredential = null

  private val removalListener: RemovalListener[String, GCMRequestTracker] =
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

  import context._
  //message processing in the beginning
  def receive:Actor.Receive = {
    case InitXmpp =>
      //create pull demand in flow
      dispatcher.getMoreCallback.invoke(appId)
      parent ! FreeConnectionAvailable

    case xmppRequest:(GcmXmppRequest, GCMRequestTracker) =>
      //first request----create connection and process request
      createConnection(xmppRequest._1.credential)
      become(free)
      self ! xmppRequest

    case other:Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in init state s")
  }

  //message processing as long as pending ack is less than max
  def free:Actor.Receive = {
    case xmppRequest:(GcmXmppRequest, GCMRequestTracker) =>
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
      processConnectionDraining()

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
      processConnectionDraining()

    case connClosed:ConnectionClosed =>
      processConnectionClosed(connClosed)

    case ackExpired:XmppAckExpired =>
      prcoessAckExpired(parent, ackExpired)
  }

  private def createConnection(credential: GoogleCredential): Unit = {
    googleCredential = credential
    createConnection(googleCredential.projectId + "@gcm.googleapis.com", googleCredential.apiKey)
  }

  private def createConnection(username:String, apiKey:String) = {
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
    val stanzaListener = new ConnektStanzaListener(self, dispatcher)
    connection.addAsyncStanzaListener(stanzaListener, stanzaFilter)

    try {
      connection.connect()
      connection.login(username, apiKey)
    }
    catch {
      case ex:Exception =>
        //TODO how to handle
        ConnektLogger(LogFile.CLIENTS).error("Unable to connect:", ex)
    }
  }

  private def processSendRequest(parent:ActorRef, xmppRequest:(GcmXmppRequest, GCMRequestTracker)) = {
    pendingAckCount = pendingAckCount + 1
    val (xmppPayload,requestTracker) = xmppRequest

    if ( sendXmppStanza(xmppPayload.pnPayload) ) {
      messageDataCache.put(xmppPayload.pnPayload.message_id, xmppRequest._2)

      if (pendingAckCount >= maxPendingAckCount) {
        become(busy)
        ConnektLogger(LogFile.CLIENTS).debug(s"XmppConnectionActor for $appId turned busy")
      } else {
        dispatcher.getMoreCallback.invoke(appId)
        parent ! FreeConnectionAvailable
      }
    }
    else
      dispatcher.retryCallback.invoke(xmppRequest)
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
    sendXmppStanza(ackMsg)
  }

  private def processDeliveryReceipt(receipt:XmppReceipt) = {
    sendUpstreamAck(receipt.from, receipt.messageId)
  }

  private def processUpstreamMessage(upstreamMsg:XmppUpstreamData) = {
    sendUpstreamAck(upstreamMsg.from, upstreamMsg.messageId)
  }

  private def processConnectionDraining() = {
    ConnektLogger(LogFile.CLIENTS).error("Received ConnectionDraining!!:", appId)
    XmppConnectionHelper.archivedConnections.add(connection)
    createConnection(googleCredential)
  }

  private def processConnectionClosed(connClosed:ConnectionClosed) = {
    ConnektLogger(LogFile.CLIENTS).error("Received Connectionclosed!!:", appId)
    XmppConnectionHelper.archivedConnections.remove(connection)
  }

  private def sendXmppStanza(payload:Any):Boolean = {
    val xmppPayloadString:String = StringUtils.objMapper.writeValueAsString(payload)
    val stanza = new GcmXmppPacketExtension(xmppPayloadString)

    try {
      connection.sendStanza(stanza)
      true
    } catch {
      case ex: NotConnectedException =>
        ConnektLogger(LogFile.CLIENTS).error("CONNECTION ERROR sending message to GCM, will be retried. jsonRequest : " + xmppPayloadString, ex)
    }
    false
  }
}
