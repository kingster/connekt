/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.busybees.models

case class GCMRequestTracker(messageId: String, deviceId: List[String], appName: String) extends HTTPRequestTracker
