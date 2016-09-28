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

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocketFactory

import akka.actor.{Actor, ActorRef}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper._
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils._
import com.google.common.cache._
import org.jivesoftware.smack.{ConnectionConfiguration, ReconnectionManager}
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.provider.{ExtensionElementProvider, ProviderManager}
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.jivesoftware.smack.util.stringencoder.java7.{Java7Base64Encoder, Java7Base64UrlSafeEncoder}
import org.xmlpull.v1.XmlPullParser

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class XmppConnectionActor(dispatcher: GcmXmppDispatcher, googleCredential: GoogleCredential, appId:String) extends Actor with Instrumented {

  val maxPendingAckCount = ConnektConfig.getInt("gcm.xmpp.maxcount").getOrElse(4)
  org.jivesoftware.smack.util.stringencoder.Base64.setEncoder(Java7Base64Encoder.getInstance())
  org.jivesoftware.smack.util.stringencoder.Base64UrlSafeEncoder.setEncoder(Java7Base64UrlSafeEncoder.getInstance())

  private val removalListener = new RemovalListener[String, (GCMRequestTracker,Long)]() {
    def onRemoval(removal: RemovalNotification[String, (GCMRequestTracker,Long)]) {
      if (removal.getCause != RemovalCause.EXPLICIT) {
        val messageData:GCMRequestTracker = removal.getValue._1
        ConnektLogger(LogFile.CLIENTS).error(s"RemoveListener:Cache timed out with tracker id ${removal.getKey}")
        self ! XmppAckExpired(messageData)
      }
    }
  }

  private val messageDataCache = CacheBuilder.newBuilder()
    .expireAfterWrite(2, TimeUnit.MINUTES)
    .removalListener(removalListener)
    .maximumSize(4096)
    .build()
    .asInstanceOf[Cache[String, (GCMRequestTracker,Long)]]


  val pendingAckCount = new AtomicInteger(0)
  var connection: XMPPTCPConnection = null

  override def postStop = {
    ConnektLogger(LogFile.CLIENTS).info("ConnectionActor:In poststop")
    if ( connection!= null && connection.isConnected) {
      connection.disconnect()
      connection.instantShutdown()
    }
    archivedConnections.foreach(conn => {conn.disconnect(); conn.instantShutdown()})
    messageDataCache.asMap().asScala.foreach(entry => ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor:PostStop:Never received GCM acknowledgement for tracker id ${entry._1}"))
  }

  import context._

  //message processing in the beginning
  def receive:Actor.Receive = {

    case xmppRequest:XmppOutStreamRequest =>
      //first request----create connection and process request
      createConnection()
      become(free)
      self ! xmppRequest

    case Shutdown =>
      become(shuttingDown)
      self ! StartShuttingDown

    case other:Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in init state")
  }

  //message processing as long as pending ack is less than max
  def free:Actor.Receive = {
    case xmppRequest:XmppOutStreamRequest =>
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

    case control: XmppControl if control.controlType.equalsIgnoreCase("CONNECTION_DRAINING") =>
        processConnectionDraining()

    case connClosed:ConnectionClosed =>
      processConnectionClosed(connClosed)

    case ackExpired:XmppAckExpired =>
      prcoessAckExpired(parent, ackExpired)

    case Shutdown =>
      become(shuttingDown)
      self ! StartShuttingDown

    case other:Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in free state s")
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

    case control: XmppControl if control.controlType.equalsIgnoreCase("CONNECTION_DRAINING") =>
    processConnectionDraining()

    case connClosed:ConnectionClosed =>
      processConnectionClosed(connClosed)

    case ackExpired:XmppAckExpired =>
      prcoessAckExpired(parent, ackExpired)

    case Shutdown =>
      become(shuttingDown)
      self ! StartShuttingDown

    case other:Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in busy state s")
  }

  def shuttingDown:Actor.Receive = {
    case StartShuttingDown =>
      ConnektLogger(LogFile.CLIENTS).info("XmppConnectionActor:Preparing to shut down")
      shutdownIfPossible()

    case ack:XmppAck =>
      processAck(parent,ack,false)
      shutdownIfPossible()

    case nack:XmppNack =>
      processNack(parent,nack,false)
      shutdownIfPossible()

    case control: XmppControl if control.controlType.equalsIgnoreCase("CONNECTION_DRAINING") =>
    processConnectionDraining(false)
      shutdownIfPossible()

    case connClosed:ConnectionClosed =>
      processConnectionClosed(connClosed)
      shutdownIfPossible()

    case ackExpired:XmppAckExpired =>
      prcoessAckExpired(parent, ackExpired, false)
      shutdownIfPossible()

    case receipt:XmppReceipt =>
      //don't do anything. When app reestablishes connection, GCM will resend
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $receipt in busy shutting s")
      shutdownIfPossible()

    case upstreamMsg:XmppUpstreamData =>
      //don't do anything. When app reestablishes connection, GCM will resend
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $upstreamMsg in shutting state s")
      shutdownIfPossible()

    case other:Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in shutting down states")
      shutdownIfPossible()
  }

  private def shutdownIfPossible(): Unit = {
    if ( pendingAckCount.get() == 0 ) {
      ConnektLogger(LogFile.CLIENTS).info("XmppConnectionActor:There is no pending ACK, shutting down")
      context stop self
    }
  }

  private def createConnection(): Unit = {
    createConnection(googleCredential.projectId + "@gcm.googleapis.com", googleCredential.apiKey)
  }

  private def createConnection(username:String, apiKey:String) = {
    connection = new XMPPTCPConnection(XMPPTCPConnectionConfiguration.builder()
                        .setHost(xmppHost)
                        .setPort(xmppPort)
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                        .setSocketFactory(SSLSocketFactory.getDefault)
                        .setServiceName(appId)
                        .setSendPresence(false)
                        .setDebuggerEnabled(true)
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
      override def accept(stanza:Stanza):Boolean = stanza.hasExtension(GCM_ELEMENT_NAME, GCM_NAMESPACE)
    }
    val stanzaListener = new ConnektStanzaListener(self, dispatcher)
    connection.addAsyncStanzaListener(stanzaListener, stanzaFilter)

    ReconnectionManager.getInstanceFor(connection).enableAutomaticReconnection()

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

  private def processSendRequest(parent:ActorRef, xmppRequest:XmppOutStreamRequest) = {
    if ( sendXmppStanza(xmppRequest.request.pnPayload) ) {
      messageDataCache.put(xmppRequest.request.pnPayload.message_id, (xmppRequest.tracker, System.currentTimeMillis()))
      pendingAckCount.incrementAndGet()

      if (pendingAckCount.get() >= maxPendingAckCount) {
        become(busy)
        parent ! ConnectionBusy
        ConnektLogger(LogFile.CLIENTS).debug(s"XmppConnectionActor for $appId turned busy")
      } else {
        parent ! FreeConnectionAvailable
      }
    }
    else {
      parent ! FreeConnectionAvailable
      dispatcher.retryCallback.invoke(xmppRequest)
    }
  }

  private def processAck(parent:ActorRef, ack:XmppAck, continue:Boolean = true) = {
    val (xmppRequestTracker,sentTime) = messageDataCache.getIfPresent(ack.messageId)
    if ( xmppRequestTracker != null ) {
      messageDataCache.invalidate(ack.messageId)
      pendingAckCount.decrementAndGet()
      dispatcher.enqueueDownstream((Success(ack), xmppRequestTracker))
      registry.timer(getMetricName(appId)).update(System.currentTimeMillis() - sentTime, TimeUnit.MILLISECONDS)

      if ( pendingAckCount.get() < maxPendingAckCount && continue) {
        become(free)
        parent ! FreeConnectionAvailable
      }
    }
  }

  private def processNack(parent:ActorRef, nack:XmppNack, continue:Boolean = true) = {
    val (xmppRequestTracker,sentTime) = messageDataCache.getIfPresent(nack.messageId)
    if ( xmppRequestTracker != null ) {
      messageDataCache.invalidate(nack.messageId)
      pendingAckCount.decrementAndGet()
      dispatcher.enqueueDownstream(Failure(new XmppNackException(nack)) -> xmppRequestTracker)
      registry.timer(getMetricName(appId)).update(System.currentTimeMillis() - sentTime, TimeUnit.MILLISECONDS)

      if ( pendingAckCount.get() < maxPendingAckCount && continue ) {
        become(free)
        parent ! FreeConnectionAvailable
      }
    }
  }

  private def prcoessAckExpired(parent:ActorRef, ackExpired:XmppAckExpired, continue:Boolean = true) = {
    if ( ackExpired.tracker != null ) {
      pendingAckCount.decrementAndGet()
      dispatcher.enqueueDownstream((Failure(new XmppNeverAckException("Ack never received. Cache timed out")), ackExpired.tracker))

      if ( pendingAckCount.get() < maxPendingAckCount && continue ) {
        become(free)
        parent ! FreeConnectionAvailable
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

  private def processConnectionDraining(continue:Boolean = true) = {
    ConnektLogger(LogFile.CLIENTS).error("Received ConnectionDraining!!:", appId)
    XmppConnectionHelper.archivedConnections.add(connection)
    if ( continue )
      createConnection()
  }

  private def processConnectionClosed(connClosed:ConnectionClosed) = {
    ConnektLogger(LogFile.CLIENTS).error("Received Connectionclosed!!:", appId)
    XmppConnectionHelper.archivedConnections.remove(connection)
  }


  private def sendXmppStanza(payload:AnyRef):Boolean = {
    val xmppPayloadString = payload.getJson
    val stanza = new GcmXmppPacketExtension(xmppPayloadString)

    try {
      connection.sendStanza(stanza)
      true
    } catch {
      case ex: Throwable =>
        ConnektLogger(LogFile.CLIENTS).error("CONNECTION ERROR sending message to GCM, will be retried. jsonRequest : " + xmppPayloadString, ex)
        false
    }
  }
}

