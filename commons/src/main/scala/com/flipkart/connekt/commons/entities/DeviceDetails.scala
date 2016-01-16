package com.flipkart.connekt.commons.entities

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
case class DeviceDetails(deviceId: String, userId: String, token: String, osName: String, osVersion: String,
                          appName: String, appVersion: String, brand: String, model: String, state: String = "")
