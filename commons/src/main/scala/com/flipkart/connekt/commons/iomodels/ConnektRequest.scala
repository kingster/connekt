package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
case class ConnektRequest(@JsonProperty(required = false) id: String,
                          channel: String,
                          sla: String,
                          templateId: String,
                          scheduleTs: Long,
                          expiryTs: Long,
                          channelInfo: ChannelRequestInfo,
                          channelData: ChannelRequestData,
                          meta: Map[String, String])
