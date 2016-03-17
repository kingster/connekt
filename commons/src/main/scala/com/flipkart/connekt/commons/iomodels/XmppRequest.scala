/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode

case class XmppRequest(to: String,
                       @JsonProperty("message_id") messageId: String,
                       data: ObjectNode,
                       @JsonProperty("time_to_live") timeToLive: String,
                       @JsonProperty("delay_while_idle") delayWhileIdle: Boolean,
                       @JsonProperty("delivery_receipt_requested") deliveryReceiptRequested: Boolean)
