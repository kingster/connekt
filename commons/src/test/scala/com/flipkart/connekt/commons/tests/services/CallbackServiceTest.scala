package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.tests.BaseCommonsTest

/**
 * @author aman.shrivastava on 10/12/15.
 */
class CallbackServiceTest extends BaseCommonsTest {
  val id: String = UUID.randomUUID().toString
  val channel: String = "push"
  val callBackEvent = PNCallbackEvent(
    messageId = UUID.randomUUID().toString,
    deviceId = UUID.randomUUID().toString,
    eventType = UUID.randomUUID().toString,
    platform = UUID.randomUUID().toString,
    appName = UUID.randomUUID().toString,
    contextId = UUID.randomUUID().toString,
    cargo = UUID.randomUUID().toString,
    timestamp = 3245567
  )

  "Callback Service" should "persist Callback Event" in {
    val callbackService = ServiceFactory.getCallbackService
    callbackService.persistCallbackEvent(id, callBackEvent.deviceId,channel, callBackEvent).isSuccess shouldEqual true
  }

  "Callback Service" should "fetchCallbackEvent" in {
    val callbackService = ServiceFactory.getCallbackService
    callbackService.fetchCallbackEvent(id, callBackEvent.deviceId, channel).isSuccess shouldEqual true
  }
}
