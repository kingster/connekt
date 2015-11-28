package com.flipkart.connekt.commons.iomodels

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
case class GCMPayload(registration_ids: List[String], delay_while_idle: Boolean, data: Any)
