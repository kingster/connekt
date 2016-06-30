package com.flipkart.connekt.busybees.xmpp

import org.jivesoftware.smack.packet.{Stanza, ExtensionElement}

/**
 * Created by subir.dey on 22/06/16.
 */
class GcmXmppPacketExtension (val json:String) extends Stanza with ExtensionElement {

  override def getNamespace:String = XmppConnectionHelper.GCM_NAMESPACE

  override def getElementName:String = XmppConnectionHelper.GCM_ELEMENT_NAME

  override def toXML(): String = {
    String.format("<%s xmlns=\"%s\">%s</%s>", getElementName, getNamespace, json,
      getElementName)
  }
}
