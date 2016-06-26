package com.flipkart.connekt.busybees.xmpp

import org.jivesoftware.smack.packet.ExtensionElement

/**
 * Created by subir.dey on 22/06/16.
 */
class GcmXmppPacketExtension (json:String) extends ExtensionElement {

  def getJsonString = json

  override def getNamespace:String = XmppConnectionActor.GCM_NAMESPACE

  override def getElementName:String = XmppConnectionActor.GCM_ELEMENT_NAME

  override def toXML(): String = {
    String.format("<%s xmlns=\"%s\">%s</%s>", getElementName, getNamespace, json,
      getElementName)
  }
}
