/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

case class EmailCallbackEvent(eventType: String, reason: String) extends CallbackEvent
