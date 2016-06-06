/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNCallbackEvent, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils._
import org.apache.commons.lang.StringUtils


class CallbackServiceTest extends CommonsBaseTest {
  val channel: String = "push"
  var mid = ""
  var callBackEvent = PNCallbackEvent(
    mid,
    clientId = StringUtils.EMPTY,
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
    contextId = None,
    client = StringUtils.EMPTY,
    channel = "push",
    sla = "H",
    templateId = Some(UUID.randomUUID().toString),
    scheduleTs = Some(System.currentTimeMillis()),
    expiryTs = Some(System.currentTimeMillis()),
    channelInfo = PNRequestInfo(platform = callBackEvent.platform,
      appName = callBackEvent.appName,
      deviceIds = Set[String](callBackEvent.deviceId),
      ackRequired = true,
      delayWhileIdle = true),
    channelData = PNRequestData(data.getObj[ObjectNode]),
    meta = Map()
  )

  "Callback Service" should "persist Callback Event" in {
    mid = ServiceFactory.getPNMessageService.saveRequest(pnInfo, "fk-connekt-pn").get
    callBackEvent = callBackEvent.copy(messageId = mid)
    pnInfo = pnInfo.copy(id = mid)

    val callbackService = ServiceFactory.getCallbackService
    callbackService.persistCallbackEvent(callBackEvent.messageId, s"${callBackEvent.appName}${callBackEvent.deviceId}", Channel.PUSH, callBackEvent).isSuccess shouldEqual true
  }

  "Callback Service" should "fetchCallbackEvent" in {
    val callbackService = ServiceFactory.getCallbackService
    val fetchCallBacks = callbackService.fetchCallbackEvent(callBackEvent.messageId, s"${callBackEvent.appName}${callBackEvent.deviceId}", Channel.PUSH)
    fetchCallBacks.isSuccess shouldEqual true
    fetchCallBacks.get.length shouldEqual 1
  }


  "Callback Service" should "fetch by contact id" in {
    val callbackService = ServiceFactory.getCallbackService
    val result = callbackService.fetchCallbackEventByContactId(s"${callBackEvent.appName}${callBackEvent.deviceId}", Channel.PUSH, System.currentTimeMillis() - 24 * 3600 * 1000, System.currentTimeMillis())
    result.isSuccess shouldEqual true
    result.get.size should be > 0
  }

  "Callback Service" should "fetch by message id" in {
    val callbackService = ServiceFactory.getCallbackService
    val result = callbackService.fetchCallbackEventByMId(callBackEvent.messageId, Channel.PUSH)
    result.isSuccess shouldEqual true
    result.get.size should be > 0
  }

  "Callback Service" should "fetch map by contact id" in {
    val callbackService = ServiceFactory.getCallbackService
    val result = callbackService.fetchEventsMapForContactId(s"${callBackEvent.appName}${callBackEvent.deviceId}", Channel.PUSH, System.currentTimeMillis() - 24 * 3600 * 1000, System.currentTimeMillis())
    result.isSuccess shouldEqual true
    result.get.size should be > 0
  }

  "Callback Servcie " should "delete " in {
    val callBackService = ServiceFactory.getCallbackService
    val events = callBackService.deleteCallBackEvent(mid, s"${callBackEvent.appName}${callBackEvent.deviceId}", Channel.PUSH)
    events.get.size == 1
  }
}
