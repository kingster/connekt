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

import org.jivesoftware.smack.packet.{Stanza, ExtensionElement}

/**
 * Created by subir.dey on 22/06/16.
 */
class GcmXmppPacketExtension (val json:String) extends Stanza with ExtensionElement {

  override def getNamespace:String = XmppConnectionHelper.GCM_NAMESPACE

  override def getElementName:String = XmppConnectionHelper.GCM_ELEMENT_NAME

  override def toXML(): String = {
    String.format("<message><%s xmlns=\"%s\">%s</%s></message>", getElementName, getNamespace, json,
      getElementName)
  }
}
