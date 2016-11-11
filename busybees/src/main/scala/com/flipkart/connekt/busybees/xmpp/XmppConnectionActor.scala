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

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, DeadLetterSuppression}
import akka.event.LoggingReceive
import com.flipkart.connekt.busybees.xmpp.Internal._
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.{SendXmppOutStreamRequest, Shutdown, StartShuttingDown}
import com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper._
import com.flipkart.connekt.commons.entities.GoogleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils._
import com.google.common.cache._
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.provider.{ExtensionElementProvider, ProviderManager}
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.jivesoftware.smack.util.stringencoder.java7.{Java7Base64Encoder, Java7Base64UrlSafeEncoder}
import org.jivesoftware.smack.{ConnectionConfiguration, ReconnectionManager}
import org.xmlpull.v1.XmlPullParser

import scala.collection.JavaConverters._
import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}

private[xmpp] class XmppConnectionActor(googleCredential: GoogleCredential, appId: String, stageLogicRef :ActorRef) extends Actor with ActorLogging with Instrumented {

  private val maxPendingAckCount = ConnektConfig.getInt("fcm.xmpp.maxPendingAcks").getOrElse(4)

  org.jivesoftware.smack.util.stringencoder.Base64.setEncoder(Java7Base64Encoder.getInstance())
  org.jivesoftware.smack.util.stringencoder.Base64UrlSafeEncoder.setEncoder(Java7Base64UrlSafeEncoder.getInstance())

  private val removalListener = new RemovalListener[String, (SendXmppOutStreamRequest, Long)]() {
    def onRemoval(removal: RemovalNotification[String, (SendXmppOutStreamRequest, Long)]) {
      if (removal.getCause != RemovalCause.EXPLICIT) {
        val messageData: SendXmppOutStreamRequest = removal.getValue._1
        ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor RemoveListener:Cache timed out with tracker id ${removal.getKey}")
        messageData.responsePromise.failure(new XmppNeverAckException(removal.getKey))
        registry.timer(s"$appId.lost").update(System.currentTimeMillis() - removal.getValue._2, TimeUnit.MILLISECONDS)
        pendingAckCount.decrementAndGet()
      }
    }
  }

  private val messageDataCache = CacheBuilder.newBuilder()
    .expireAfterWrite(2, TimeUnit.MINUTES)
    .removalListener(removalListener)
    .maximumSize(4096)
    .build()
    .asInstanceOf[Cache[String, (SendXmppOutStreamRequest, Long)]]

  val pendingAckCount = new AtomicInteger(0)
  var connection: XMPPTCPConnection = _

  val archivedConnections = new scala.collection.mutable.HashSet[XMPPTCPConnection]()

  override def postStop = {
    ConnektLogger(LogFile.CLIENTS).info("ConnectionActor:In poststop")
    if (connection != null && connection.isConnected) {
      connection.disconnect()
      connection.instantShutdown()
    }
    archivedConnections.foreach(conn => {
      conn.disconnect(); conn.instantShutdown()
    })
    messageDataCache.asMap().asScala.foreach { entry =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor:PostStop:Never received GCM acknowledgement for tracker id ${entry._1}")
      entry._2._1.responsePromise.failure(StoppingException())
    }
  }

  import context._

  override def preStart(): Unit = {
    super.preStart()
    connection = createConnection(googleCredential)
  }

  //message processing as long as pending ack is less than max
  def receive: Actor.Receive = LoggingReceive {
    case xmppRequest: SendXmppOutStreamRequest =>
      processSendRequest(parent, xmppRequest)

    case XmppRequestAvailable =>
      parent ! FreeConnectionAvailable

    case ack: XmppAck =>
      processAck(ack)
      tryHungry()

    case nack: XmppNack =>
      processNack(nack)
      tryHungry()

    case receipt: XmppReceipt =>
      processDeliveryReceipt(receipt)

    case upstreamMsg: XmppUpstreamData =>
      processUpstreamMessage(upstreamMsg)

    case control: XmppControl if control.controlType.equalsIgnoreCase("CONNECTION_DRAINING") =>
      processConnectionDraining()
      connection = createConnection(googleCredential)

    case connClosed: ConnectionClosed =>
      processConnectionClosed(connClosed)

    case shutdown: Shutdown =>
      become(shuttingDown)
      self ! StartShuttingDown(shutdown)

    case other: Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in free state s")
  }

  //message processing when pendingack has reached max
  def busy: Actor.Receive = LoggingReceive {

    case xmppRequest: SendXmppOutStreamRequest =>
      xmppRequest.responsePromise.failure(new Throwable("XMPP_BUSY"))
      //This shouldn't have happened.

    case XmppRequestAvailable => //do nothing

    case ack: XmppAck =>
      processAck(ack)
      tryHungry()

    case nack: XmppNack =>
      processNack(nack)
      tryHungry()

    case receipt: XmppReceipt =>
      processDeliveryReceipt(receipt)

    case upstreamMsg: XmppUpstreamData =>
      processUpstreamMessage(upstreamMsg)

    case control: XmppControl if control.controlType.equalsIgnoreCase("CONNECTION_DRAINING") =>
      processConnectionDraining()
      connection = createConnection(googleCredential)


    case connClosed: ConnectionClosed =>
      processConnectionClosed(connClosed)

    case shutdown: Shutdown =>
      become(shuttingDown)
      self ! StartShuttingDown(shutdown)

    case other: Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in busy state s")
  }

  def shuttingDown: Actor.Receive = LoggingReceive {
    case xmppRequest: SendXmppOutStreamRequest =>
      xmppRequest.responsePromise.failure(StoppingException())

    case XmppRequestAvailable => //do nothing

    case StartShuttingDown(shutdown) if shutdown.discardPendingAcks =>
      ConnektLogger(LogFile.CLIENTS).info("XmppConnectionActor:Hard Shutdown....")
      context.stop(self)

    case StartShuttingDown(shutdown) if !shutdown.discardPendingAcks =>
      ConnektLogger(LogFile.CLIENTS).info("XmppConnectionActor:StartShuttingDown..... Will wait for acts to complete")
      shutdownIfPossible()
    //      context.system.scheduler.schedule(0.seconds, 30.seconds) {
    //        //self ! PoisonPill
    //        ConnektLogger(LogFile.CLIENTS).info("XmppConnectionActor:Hard Shutdown....")
    //        context.stop(self)
    //      }

    case ack: XmppAck =>
      processAck(ack)
      shutdownIfPossible()

    case nack: XmppNack =>
      processNack(nack)
      shutdownIfPossible()

    case control: XmppControl if control.controlType.equalsIgnoreCase("CONNECTION_DRAINING") =>
      processConnectionDraining()
      shutdownIfPossible()

    case connClosed: ConnectionClosed =>
      processConnectionClosed(connClosed)
      shutdownIfPossible()

    case upstream: XmppUpstreamResponse =>
      //don't do anything. When app reestablishes connection, GCM will resend
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $upstream in busy shutting s")
      shutdownIfPossible()

    case other: Any =>
      ConnektLogger(LogFile.CLIENTS).error(s"XmppConnectionActor received $other in shutting down states")
      shutdownIfPossible()
  }


  private def shutdownIfPossible(): Unit = {
    if (pendingAckCount.get() == 0) {
      ConnektLogger(LogFile.CLIENTS).info("XmppConnectionActor:There is no pending ACK, shutting down")
      context.stop(self)
    }
  }

  private def sendUpstreamAck(to: String, messageId: String) = {
    val ackMsg = XmppUpstreamAck(messageId, "ack", to)
    sendXmppStanza(ackMsg)
  }

  private def processDeliveryReceipt(receipt: XmppReceipt) = {
    sendUpstreamAck(receipt.from, receipt.messageId)
  }

  private def processUpstreamMessage(upstreamMsg: XmppUpstreamData) = {
    sendUpstreamAck(upstreamMsg.from, upstreamMsg.messageId)
  }


  private def sendXmppStanza(payload: AnyRef): Try[Unit] = {
    val xmppPayloadString = payload.getJson
    val stanza = new GcmXmppPacketExtension(xmppPayloadString)

    try {
      connection.sendStanza(stanza)
      Success(Unit)
    } catch {
      case ex: Throwable =>
        ConnektLogger(LogFile.CLIENTS).error("CONNECTION ERROR sending message to GCM, will be retried. jsonRequest : " + xmppPayloadString, ex)
        Failure(ex)
    }
  }

  //########## Kinshuk Done #############

  private def processSendRequest(routerRef: ActorRef, xmppRequest: SendXmppOutStreamRequest) = {
    sendXmppStanza(xmppRequest.request.pnPayload) match {
      case Success(_) =>
        messageDataCache.put(xmppRequest.request.pnPayload.message_id, (xmppRequest, System.currentTimeMillis()))
        pendingAckCount.incrementAndGet()

        if (pendingAckCount.get() >= maxPendingAckCount) {
          become(busy)
          parent ! ConnectionBusy
          ConnektLogger(LogFile.CLIENTS).debug(s"XmppConnectionActor for $appId turned busy")
        } else {
          parent ! FreeConnectionAvailable
        }
      case Failure(ex) =>
        xmppRequest.responsePromise.failure(ex)
        parent ! FreeConnectionAvailable
    }
  }

  private def processAck(ack: XmppAck) = {
    val (outStreamRequest, sentTime) = messageDataCache.getIfPresent(ack.messageId)
    if (outStreamRequest != null) {
      messageDataCache.invalidate(ack.messageId)
      pendingAckCount.decrementAndGet()
      outStreamRequest.responsePromise.success(ack)
      registry.timer(s"$appId.ack").update(System.currentTimeMillis() - sentTime, TimeUnit.MILLISECONDS)
    }
  }

  private def processNack(nack: XmppNack) = {
    val (outStreamRequest, sentTime) = messageDataCache.getIfPresent(nack.messageId)
    if (outStreamRequest != null) {
      messageDataCache.invalidate(nack.messageId)
      pendingAckCount.decrementAndGet()
      outStreamRequest.responsePromise.failure(new XmppNackException(nack))
      registry.timer(s"$appId.nack").update(System.currentTimeMillis() - sentTime, TimeUnit.MILLISECONDS)
    }
  }


  private def tryHungry(): Unit = {
    if (pendingAckCount.get() < maxPendingAckCount) {
      become(receive) // What happens when already in recieve
      parent ! FreeConnectionAvailable
    }
  }

  private def processConnectionDraining() = {
    ConnektLogger(LogFile.CLIENTS).warn(s"Received ConnectionDraining for $appId")
    ReconnectionManager.getInstanceFor(connection).disableAutomaticReconnection()
    archivedConnections.add(connection)
  }


  /**
    * Incase current connection is closed, it will attempt reconnect.
    *
    * @param connClosed
    * @return
    */
  private def processConnectionClosed(connClosed: ConnectionClosed) = {
    ConnektLogger(LogFile.CLIENTS).warn("Received Connection Closed!!:", appId)
    archivedConnections.remove(connClosed.connection)
  }

  /**
    *
    * @param googleCredential
    */
  private def createConnection(googleCredential: GoogleCredential): XMPPTCPConnection = {
    val connection = new XMPPTCPConnection(XMPPTCPConnectionConfiguration.builder()
      .setHost(xmppHost)
      .setPort(xmppPort)
      .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
      .setSocketFactory(SSLSocketFactory.getDefault)
      .setServiceName(appId)
      .setSendPresence(false)
      .setDebuggerEnabled(false)
      .build())

    ConnektLogger(LogFile.CLIENTS).debug(s"Configuring XMPPConnection")
    Roster.getInstanceFor(connection).setRosterLoadedAtLogin(false)
    connection.addConnectionListener(new ConnektXmppConnectionListener(connection, self))

    ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
      new ExtensionElementProvider[GcmXmppPacketExtension]() {
        override def parse(parser: XmlPullParser, initialDepth: Int): GcmXmppPacketExtension = {
          val json: String = parser.nextText()
          new GcmXmppPacketExtension(json)
        }
      }
    )

    val stanzaFilter = new StanzaFilter() {
      override def accept(stanza: Stanza): Boolean = stanza.hasExtension(GCM_ELEMENT_NAME, GCM_NAMESPACE)
    }
    val stanzaListener = new ConnektStanzaListener(self, stageLogicRef)
    connection.addAsyncStanzaListener(stanzaListener, stanzaFilter)

    ReconnectionManager.getInstanceFor(connection).enableAutomaticReconnection()

    try {
      connection.connect()
      connection.login(googleCredential.projectId + "@gcm.googleapis.com", googleCredential.apiKey)
      connection
    }
    catch {
      case ex: Exception =>
        //TODO how to handle
        ConnektLogger(LogFile.CLIENTS).error("Unable to connect:", ex)
        throw ex
    }

  }

}

private[xmpp] object XmppConnectionActor {

  final case class SendXmppOutStreamRequest(request: FCMXmppRequest, responsePromise: Promise[XmppDownstreamResponse])

  final case class Shutdown(discardPendingAcks: Boolean, shutdownCompletedPromise: Promise[Done]) extends DeadLetterSuppression

  sealed case class StartShuttingDown(shutdown: Shutdown) extends DeadLetterSuppression

  final case class PendingUpstreamMessage(actTo:ActorRef, message:XmppUpstreamResponse)

}

