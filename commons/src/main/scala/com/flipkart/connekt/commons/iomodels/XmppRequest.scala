package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
case class XmppRequest(to: String,
                       @JsonProperty("message_id") messageId: String,
                       data: ObjectNode,
                       @JsonProperty("time_to_live") timeToLive: String,
                       @JsonProperty("delay_while_idle") delayWhileIdle: Boolean,
                       @JsonProperty("delivery_receipt_requested") deliveryReceiptRequested: Boolean)
