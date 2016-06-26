package com.flipkart.connekt.busybees.xmpp

import akka.actor.ActorRef
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.{XmppResponse, XmppUpstreamData}
import com.google.gson.JsonObject
import org.apache.commons.lang.StringUtils
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.packet.Stanza
import com.google.gson.{Gson, GsonBuilder, JsonObject, JsonParser}

/**
 * Created by subir.dey on 22/06/16.
 */
class ConnektStanzaListener(connectionActor:ActorRef) extends StanzaListener() {


  override def processPacket(packet: Stanza)  {
    // Extract the GCM message from the packet.
    val packetExtension:GcmXmppPacketExtension = packet.getExtension(XmppConnectionActor.GCM_NAMESPACE).asInstanceOf[GcmXmppPacketExtension]
    val jsonGcmDownstreamMessage:XmppResponse = XmppConnectionActor.mapper.readValue(packetExtension.getJsonString, classOf[XmppResponse])
    if (jsonGcmDownstreamMessage != null)
      connectionActor ! jsonGcmDownstreamMessage
    else
      connectionActor ! XmppConnectionActor.mapper.readValue(packetExtension.getJsonString, classOf[XmppUpstreamData])
  }
}
