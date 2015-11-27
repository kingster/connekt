package com.flipkart.connekt.commons.iomodels

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
case class ConnektRequest(channel: String, sla: String, templateId: String, scheduleTs: Long, expiryTs: Long,
                          data: ChannelRequestData, meta: Map[String, String])
