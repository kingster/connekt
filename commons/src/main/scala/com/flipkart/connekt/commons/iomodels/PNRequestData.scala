package com.flipkart.connekt.commons.iomodels

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
case class PNRequestData(requestId: String, appName: String, deviceId: String, data: String,
                         ackRequired: Boolean, delayWhileIdle: Boolean) extends ChannelRequestData
