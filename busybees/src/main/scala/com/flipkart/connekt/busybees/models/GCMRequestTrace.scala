package com.flipkart.connekt.busybees.models

/**
 * Created by kinshuk.bairagi on 25/02/16.
 */
case class GCMRequestTrace(messageId: String, deviceId: List[String], appName: String) extends HTTPRequestTracker
