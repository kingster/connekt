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
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.PendingUpstreamMessage
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.packet.Stanza

import scala.util.{Failure, Success, Try}

private [xmpp] class ConnektStanzaListener(connectionActor:ActorRef, stageLogicRef:ActorRef) extends StanzaListener() {

  override def processPacket(packet: Stanza)  {
    // Extract the GCM message from the packet.
    val packetExtension:GcmXmppPacketExtension = packet.getExtension(Internal.GCM_NAMESPACE).asInstanceOf[GcmXmppPacketExtension]
    //ConnektLogger(LogFile.CLIENTS).trace("Response from GCM:" + packetExtension.json)

    Try (packetExtension.json.getObj[XmppResponse]) match {
      case Success(downStreamMsg:XmppDownstreamResponse) =>
        ConnektLogger(LogFile.CLIENTS).debug("De Serialised to downstream:" + downStreamMsg)
        connectionActor ! downStreamMsg
      case Success(upstream:XmppUpstreamResponse) =>
        ConnektLogger(LogFile.CLIENTS).debug("De Serialised to upstream:" + upstream)
        stageLogicRef ! PendingUpstreamMessage(connectionActor,upstream)
      case Failure(thrown) =>
        ConnektLogger(LogFile.CLIENTS).error("Failed to down/upstream:" + packetExtension.json)
      case _ =>
        ConnektLogger(LogFile.CLIENTS).error("StanzaListener Unhandled Match:" + packetExtension.json)

    }
  }
}

