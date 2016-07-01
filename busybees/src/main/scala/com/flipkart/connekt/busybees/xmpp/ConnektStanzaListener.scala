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
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.commons.iomodels.{XmppUpstreamResponse, XmppReceipt, XmppDownstreamResponse, XmppUpstreamData}
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.packet.Stanza

/**
 * Created by subir.dey on 22/06/16.
 */
class ConnektStanzaListener(connectionActor:ActorRef, dispatcher:GcmXmppDispatcher) extends StanzaListener() {


  override def processPacket(packet: Stanza)  {
    // Extract the GCM message from the packet.
    val packetExtension:GcmXmppPacketExtension = packet.getExtension(XmppConnectionHelper.GCM_NAMESPACE).asInstanceOf[GcmXmppPacketExtension]
    val jsonGcmDownstreamMessage:XmppDownstreamResponse = packetExtension.json.getObj[XmppDownstreamResponse]
    if (jsonGcmDownstreamMessage != null)
        connectionActor ! jsonGcmDownstreamMessage
    else {
      var jsonGcmUpstreamMessage:XmppUpstreamResponse = packetExtension.json.getObj[XmppUpstreamResponse]
      if ( jsonGcmUpstreamMessage == null )
        jsonGcmUpstreamMessage = packetExtension.json.getObj[XmppUpstreamData]
      dispatcher.upStreamRecvdCallback.invoke(connectionActor -> packetExtension.json.getObj[XmppUpstreamData])
    }
  }
}
