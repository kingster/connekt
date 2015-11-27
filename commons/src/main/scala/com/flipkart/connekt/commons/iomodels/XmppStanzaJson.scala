package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
case class XmppStanzaJson(to: String, message_id: String, data: ObjectNode, time_to_live: String, delay_while_idle: Boolean, delivery_receipt_requested: Boolean)
