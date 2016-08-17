package com.flipkart.connekt.commons.tests.iomodel

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._
import org.scalatest.FlatSpec

class XmppDeserialiseTest extends FlatSpec {
  "create Receipt Test " should "run" in {
    val drJsonString = "{\"data\":{\"message_status\":\"MESSAGE_SENT_TO_DEVICE\",\"device_registration_id\":\"dbp9UlEJTbg:APA91bHZxuX6BnPLW4J_NZSLh7TeXN00eOGZY99npmVBVpB8MVciZDHlhOL4F7RRCktfcYQf90_7HgsWvLLh8C4aavinu6Mp0nt3RKCvy0EaopnRDqK_XwFwTeOkb8SAeQUCBwbmGPXA\",\"message_sent_timestamp\":\"1468231318886\",\"original_message_id\":\"1468231313418:subirDeviceForXmppTest:\"},\"time_to_live\":0,\"from\":\"gcm.googleapis.com\",\"message_id\":\"dr2:1468231313418:subirDeviceForXmppTest:\",\"message_type\":\"receipt\",\"category\":\"com.flipkart.android.connekt.example\"}"
    val drMsg: XmppResponse = drJsonString.getObj[XmppResponse]
    ConnektLogger(LogFile.CLIENTS).debug(drMsg)
    assert(drMsg.isInstanceOf[XmppReceipt])
  }

  "create Upstream" should "run" in {
    val upJsonString = "{\"data\":{\"appName\":\"ConnektSampleApp\",\"eventType\":\"RECEIVED\",\"type\":\"PN\",\"cargo\":\"{\\\"numFKNotificationInTray\\\":\\\"0\\\",\\\"totalDiskSpace\\\":\\\"13851541504\\\",\\\"emptyDiskSpace\\\":\\\"3044192256\\\",\\\"networkType\\\":\\\"WiFi\\\",\\\"serviceProvider\\\":\\\"Vodafone IN\\\"}\",\"deviceId\":\"dbp9UlEJTbg:APA91bHZxuX6BnPLW4J_NZSLh7TeXN00eOGZY99npmVBVpB8MVciZDHlhOL4F7RRCktfcYQf90_7HgsWvLLh8C4aavinu6Mp0nt3RKCvy0EaopnRDqK_XwFwTeOkb8SAeQUCBwbmGPXA\",\"timestamp\":\"1468231318619\"},\"time_to_live\":0,\"from\":\"dbp9UlEJTbg:APA91bHZxuX6BnPLW4J_NZSLh7TeXN00eOGZY99npmVBVpB8MVciZDHlhOL4F7RRCktfcYQf90_7HgsWvLLh8C4aavinu6Mp0nt3RKCvy0EaopnRDqK_XwFwTeOkb8SAeQUCBwbmGPXA\",\"message_id\":\"1468231313418:subirDeviceForXmppTest:\",\"category\":\"com.flipkart.android.connekt.example\"}"
    val upstreamMsg: XmppResponse = upJsonString.getObj[XmppResponse]
    ConnektLogger(LogFile.CLIENTS).debug(upstreamMsg)
    assert(upstreamMsg.isInstanceOf[XmppUpstreamData])
  }

  "create Nack" should "run" in {
    val nackString = "{\"message_type\":\"nack\", \"message_id\":\"msgId1\",\"from\":\"bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1...\",\"error\":\"INVALID_JSON\",\"error_description\":\"InvalidJson: JSON_TYPE_ERROR : Field \\\"time_to_live\\\" must be a JSON java.lang.Number: abc\"}"
    val nackMsg:XmppResponse = nackString.getObj[XmppResponse]
    ConnektLogger(LogFile.CLIENTS).debug(nackMsg)
    assert(nackMsg.isInstanceOf[XmppDownstreamResponse])
  }
}
