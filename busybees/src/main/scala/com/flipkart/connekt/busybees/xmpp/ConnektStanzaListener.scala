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
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.commons.iomodels._
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.packet.Stanza

import scala.util.{Try, Success, Failure}

class ConnektStanzaListener(connectionActor:ActorRef, dispatcher:GcmXmppDispatcher) extends StanzaListener() {

  override def processPacket(packet: Stanza)  {
    // Extract the GCM message from the packet.
    val packetExtension:GcmXmppPacketExtension = packet.getExtension(XmppConnectionHelper.GCM_NAMESPACE).asInstanceOf[GcmXmppPacketExtension]
    ConnektLogger(LogFile.CLIENTS).debug("Response from GCM:" + packetExtension.json)

    Try (packetExtension.json.getObj[XmppResponse]) match {
      case Success(downStreamMsg:XmppDownstreamResponse) =>
        ConnektLogger(LogFile.CLIENTS).debug("De Serialised to downstreamobject:" + downStreamMsg)
        connectionActor ! downStreamMsg
      case Success(upstream:XmppUpstreamResponse) =>
        ConnektLogger(LogFile.CLIENTS).debug("De Serialised to upstream:" + upstream)
        dispatcher.upStreamRecvdCallback.invoke(connectionActor -> upstream)
      case Failure(thrown) =>
        ConnektLogger(LogFile.CLIENTS).error("Failed to down/upstream:" + packetExtension.json)
    }
  }
}
