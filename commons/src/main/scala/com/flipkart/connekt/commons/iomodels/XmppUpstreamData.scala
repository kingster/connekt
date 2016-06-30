package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Created by subir.dey on 23/06/16.
 */
case class XmppUpstreamData (
                              @JsonProperty("message_id")@JsonProperty(required = true) override val messageId: String,
                              @JsonProperty(required = true) override val from: String,
                              @JsonProperty(required = true) override val category: String,
                              @JsonProperty(required = false) data: ObjectNode) extends XmppUpstreamResponse(messageId, from, category)

/** Sample from GCM
  * <message id="">
  *   <gcm xmlns="google:mobile:data">
  *     {
  *     "category":"com.example.yourapp", // to know which app sent it
  *     "data":
  *     {
  *     "hello":"world",
  *     },
  *     "message_id":"m-123",
  *     "from":"REGID"
  *     }
  *     </gcm>
  *     </message>
*/
