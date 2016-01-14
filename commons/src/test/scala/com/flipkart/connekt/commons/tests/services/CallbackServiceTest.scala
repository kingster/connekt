package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.PNCallbackEvent
import com.flipkart.connekt.commons.tests.BaseCommonsTest
import com.flipkart.connekt.commons.utils.StringUtils._
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
//
//  "Callback Service" should "persist Callback Event" in {
//    val callbackService = ServiceFactory.getCallbackService
//    callbackService.persistCallbackEvent(id, callBackEvent.deviceId,channel, callBackEvent).isSuccess shouldEqual true
//  }
//
//  "Callback Service" should "fetchCallbackEvent" in {
//    val callbackService = ServiceFactory.getCallbackService
//    callbackService.fetchCallbackEvent(id, callBackEvent.deviceId, channel).isSuccess shouldEqual true
//  }
//
//  "Callback Service" should "fetch by contact id" in {
//    val callbackService = ServiceFactory.getCallbackService
//    val result = callbackService.fetchCallbackEventByContactId("d7ae09474408d039ecad4534ed040f4a", "push")
//    print(result.get.getJson)
//    result.isSuccess shouldEqual true
//    result.get.size should be > 0
//  }
//
//  "Callback Service" should "fetch by message id" in {
//    val callbackService = ServiceFactory.getCallbackService
//    val result = callbackService.fetchCallbackEventByMId("44964fe6-2d18-4da2-aada-0a10db0b08f1", "push")
//    println(result.get.getJson)
//    result.isSuccess shouldEqual true
//    result.get.size should be > 0
//  }

  "Callback Service" should "fetch map by contact id" in {
    val callbackService = ServiceFactory.getCallbackService
    val result = callbackService.fetchEventsMapForContactId("d7ae09474408d039ecad4534ed040f4a", "push")
    print(result.get.getJson)
    result.isSuccess shouldEqual true
    result.get.size should be > 0
  }


}
