package com.flipkart.connekt.busybees.xmpp

import akka.actor.ActorRef
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.flipkart.connekt.busybees.streams.flows.dispatchers.GcmXmppDispatcher
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.{XmppReceipt, XmppResponse, XmppUpstreamData}
import com.google.gson.JsonObject
import org.apache.commons.lang.StringUtils
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.packet.Stanza
import com.google.gson.{Gson, GsonBuilder, JsonObject, JsonParser}

/**
 * Created by subir.dey on 22/06/16.
 */
class ConnektStanzaListener(connectionActor:ActorRef, dispatcher:GcmXmppDispatcher) extends StanzaListener() {


  override def processPacket(packet: Stanza)  {
    // Extract the GCM message from the packet.
    val packetExtension:GcmXmppPacketExtension = packet.getExtension(XmppConnectionActor.GCM_NAMESPACE).asInstanceOf[GcmXmppPacketExtension]
    val jsonGcmDownstreamMessage:XmppResponse = com.flipkart.connekt.commons.utils.StringUtils.objMapper.readValue(packetExtension.getJsonString, classOf[XmppResponse])
    if (jsonGcmDownstreamMessage != null) {
      if ( jsonGcmDownstreamMessage.isInstanceOf[XmppReceipt] )
        dispatcher.drRecvdCallback.invoke(connectionActor, jsonGcmDownstreamMessage.asInstanceOf[XmppReceipt])
      else
        connectionActor ! jsonGcmDownstreamMessage
    }
    else
      dispatcher.upStreamRecvdCallback.invoke(connectionActor, com.flipkart.connekt.commons.utils.StringUtils.objMapper.readValue(packetExtension.getJsonString, classOf[XmppUpstreamData]))
  }
}
