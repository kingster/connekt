package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNCallbackEvent, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.tests.BaseCommonsTest
import com.flipkart.connekt.commons.utils.StringUtils._


/**
 * @author aman.shrivastava on 10/12/15.
 */
class CallbackServiceTest extends BaseCommonsTest {
  val channel: String = "push"
  var mid = ""
  var callBackEvent = PNCallbackEvent(
    mid,
    deviceId = UUID.randomUUID().toString,
    eventType = UUID.randomUUID().toString,
    platform = UUID.randomUUID().toString,
    appName = UUID.randomUUID().toString,
    contextId = UUID.randomUUID().toString,
    cargo = UUID.randomUUID().toString,
    timestamp = 3245567
  )
  val data = "{        \"message\": \"Hello World\",        \"title\": \"Hello world\",        \"id\": \"pqwx2p2x321122228w2t1wxt\",        \"triggerSound\" : true,        \"notificationType\" : \"Text\"}"

  var pnInfo = ConnektRequest(
    mid,
    channel = "push",
    sla = "H",
    templateId = UUID.randomUUID().toString,
    scheduleTs = System.currentTimeMillis(),
    expiryTs = System.currentTimeMillis(),
    channelInfo = PNRequestInfo(platform = callBackEvent.platform,
                      appName = callBackEvent.appName,
                      deviceId = callBackEvent.deviceId,
                      ackRequired = true,
                      delayWhileIdle = true),
    channelData = PNRequestData(data.getObj[ObjectNode]),
    meta = Map()
  )

  "Callback Service" should "persist Callback Event" in {
    mid = ServiceFactory.getMessageService.persistRequest(pnInfo, "fk-connekt-pn", true).get
    callBackEvent = callBackEvent.copy(messageId = mid)
    pnInfo = pnInfo.copy(id = mid)

    val callbackService = ServiceFactory.getCallbackService
    callbackService.persistCallbackEvent(callBackEvent.messageId, callBackEvent.deviceId, Channel.PN, callBackEvent).isSuccess shouldEqual true
  }

  "Callback Service" should "fetchCallbackEvent" in {
    val callbackService = ServiceFactory.getCallbackService
    callbackService.fetchCallbackEvent(callBackEvent.messageId, callBackEvent.deviceId, Channel.PN).isSuccess shouldEqual true
  }

  "Callback Service" should "fetch by contact id" in {
    val callbackService = ServiceFactory.getCallbackService
    val result = callbackService.fetchCallbackEventByContactId(callBackEvent.deviceId, Channel.PN, System.currentTimeMillis() - 24 * 3600 * 1000, System.currentTimeMillis())
    print(result.get.getJson)
    result.isSuccess shouldEqual true
    result.get.size should be > 0
  }

  "Callback Service" should "fetch by message id" in {
    val callbackService = ServiceFactory.getCallbackService
    val result = callbackService.fetchCallbackEventByMId(callBackEvent.messageId, Channel.PN)
    println(result.get.getJson)
    result.isSuccess shouldEqual true
    result.get.size should be > 0
  }

  "Callback Service" should "fetch map by contact id" in {
    val callbackService = ServiceFactory.getCallbackService
    val result = callbackService.fetchEventsMapForContactId(callBackEvent.deviceId, Channel.PN, System.currentTimeMillis() - 24 * 3600 * 1000, System.currentTimeMillis())
    print(result.get.getJson)
    result.isSuccess shouldEqual true
    result.get.size should be > 0
  }


}
